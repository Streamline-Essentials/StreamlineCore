package host.plas.discord.messaging;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import host.plas.StreamlineDiscord;
import host.plas.config.Messages;
import host.plas.config.Messaging;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reloads and resets the files the bot's messages come from: {@code messages.yml}, {@code messaging.yml} and
 * the shipped embeds in {@code embeds/}.
 *
 * <p>A reset puts back the copy shipped in the module jar. The file it replaces is first copied to
 * {@code backups/message-resets/<time>/}, so an owner's edits can always be recovered. Embeds the owner made
 * themselves have no shipped copy and are never touched.</p>
 */
public final class MessageFiles {
    public static final String MESSAGES = "messages.yml";
    public static final String MESSAGING = "messaging.yml";
    public static final String ALL = "ALL";

    private MessageFiles() {
    }

    /** What a reset or reload did, as lines for the sender. */
    public static final class Result {
        public final List<String> done = new ArrayList<>();
        public final List<String> failed = new ArrayList<>();
        public File backupFolder;
    }

    /** Every file name {@link #reset} takes besides {@link #ALL}: the two YAML files, then the shipped embeds. */
    public static Set<String> resettable() {
        Set<String> all = new LinkedHashSet<>();
        all.add(MESSAGES);
        all.add(MESSAGING);
        for (String embed : BotMessages.getDefaultEmbeds()) {
            if (Embeds.readShipped(embed) != null) all.add(embed);
        }
        return all;
    }

    /**
     * The resettable file {@code input} names, or null when it names none. {@code messages}, {@code messages.yml},
     * {@code on-death}, {@code on-death.json} and {@code embeds/on-death.json} are all accepted, in any case.
     */
    public static String match(String input) {
        if (input == null) return null;
        String name = input.trim().replace('\\', '/');
        if (name.toLowerCase(Locale.ROOT).startsWith("embeds/")) name = name.substring("embeds/".length());
        if (name.isEmpty()) return null;

        for (String file : resettable()) {
            if (file.equalsIgnoreCase(name)) return file;
            int dot = file.lastIndexOf('.');
            if (dot > 0 && file.substring(0, dot).equalsIgnoreCase(name)) return file;
        }
        return null;
    }

    /**
     * Restores {@code fileName} (a name from {@link #resettable()}), or every one of them for {@link #ALL}, to
     * the shipped default, then registers the bot's messages again so keys the jar's copy lacks are filled in.
     */
    public static synchronized Result reset(String fileName) {
        Result result = new Result();
        File dataFolder = StreamlineDiscord.getInstance().getDataFolder();
        result.backupFolder = new File(dataFolder, "backups" + File.separator + "message-resets" + File.separator
                + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()));

        List<String> targets = new ArrayList<>();
        if (ALL.equalsIgnoreCase(fileName)) targets.addAll(resettable());
        else targets.add(fileName);

        boolean yaml = false;
        for (String target : targets) {
            boolean isYaml = target.equals(MESSAGES) || target.equals(MESSAGING);
            File file = isYaml ? new File(dataFolder, target) : new File(Embeds.getFolder(), target);
            String shown = isYaml ? target : "embeds/" + target;

            try {
                if (file.exists()) {
                    File backup = new File(result.backupFolder, isYaml ? target : "embeds" + File.separator + target);
                    backup.getParentFile().mkdirs();
                    Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.delete(file.toPath());
                }

                if (target.equals(MESSAGES)) StreamlineDiscord.setMessages(new Messages());
                else if (target.equals(MESSAGING)) StreamlineDiscord.setMessaging(new Messaging());
                else Embeds.ensureShipped(target);

                if (isYaml) yaml = true;
                else if (! file.exists()) throw new IllegalStateException("the module jar holds no copy of it");
                result.done.add(shown);
            } catch (Exception e) {
                result.failed.add(shown + ": " + e.getMessage());
                StreamlineDiscord.getInstance().logWarning("Could not reset " + shown + ": " + e);
            }
        }

        if (yaml) BotMessages.registerDefaults();
        if (! result.backupFolder.exists()) result.backupFolder = null;
        return result;
    }

    /**
     * Loads {@code messages.yml} and {@code messaging.yml} again, fills in missing keys and shipped embeds, and
     * checks every embed file. {@code done} lists what was reloaded, {@code failed} the embeds that would not
     * send.
     */
    public static synchronized Result reload() {
        Result result = new Result();

        StreamlineDiscord.setMessages(new Messages());
        StreamlineDiscord.setMessaging(new Messaging());
        BotMessages.registerDefaults();
        result.done.add(MESSAGES);
        result.done.add(MESSAGING);

        File[] embeds = Embeds.getFolder().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
        int count = 0;
        if (embeds != null) {
            for (File embed : embeds) {
                count++;
                String problem = check(embed);
                if (problem != null) result.failed.add("embeds/" + embed.getName() + ": " + problem);
            }
        }
        result.done.add(count + " embed file(s)");
        return result;
    }

    /** Why an embed file cannot be sent, or null when it reads as a JSON object. */
    private static String check(File file) {
        try {
            JsonElement element = new JsonParser().parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            return element != null && element.isJsonObject() ? null : "it must hold a JSON object ({ ... })";
        } catch (Exception e) {
            return "not valid JSON (" + e.getMessage() + ")";
        }
    }
}
