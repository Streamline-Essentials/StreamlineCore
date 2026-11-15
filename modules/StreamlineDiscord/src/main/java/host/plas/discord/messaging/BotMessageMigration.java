package host.plas.discord.messaging;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import host.plas.StreamlineDiscord;
import host.plas.config.Messages;
import host.plas.config.Messaging;
import singularity.configs.ModularizedConfig;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Moves settings from the layout where every message named a {@code --file:<name>.json} in
 * {@code forwarded-jsons/} or {@code discord-commands/<command>/} onto {@code embeds/},
 * {@code messages.yml} and {@code messaging.yml}.
 *
 * <ul>
 *     <li>JSON files in those folders that differ from the shipped default of the same name are copied into
 *     {@code embeds/} (renamed when {@code embeds/} already holds a different file of that name). Every old
 *     JSON file is then moved to {@code backups/old-embeds/}, so nothing is deleted and the migration runs once.</li>
 *     <li>A {@code --file:} value in {@code messages.yml} or in a command's {@code messages.reply.*} becomes the
 *     message's {@code messaging.yml} entry; a plain-text value becomes its {@code messages.yml} text with the
 *     {@code default} setting.</li>
 * </ul>
 */
public final class BotMessageMigration {
    /** Old {@code messages.yml} keys and the bot message keys they became. */
    private static final Map<String, String> OLD_MESSAGE_KEYS = new LinkedHashMap<>();

    static {
        OLD_MESSAGE_KEYS.put("verification.success.discord", "verification.success.discord");
        OLD_MESSAGE_KEYS.put("verification.failure.generic", "verification.failure.generic");
        OLD_MESSAGE_KEYS.put("verification.failure.already-verified", "verification.failure.already-verified");
        OLD_MESSAGE_KEYS.put("unverification.success.discord", "unverification.success.discord");
        OLD_MESSAGE_KEYS.put("unverification.failure.generic", "unverification.failure.generic");
        OLD_MESSAGE_KEYS.put("unverification.failure.already-unverified", "unverification.failure.already-unverified");
        OLD_MESSAGE_KEYS.put("forwarded.streamline.login", "events.join");
        OLD_MESSAGE_KEYS.put("forwarded.streamline.logout", "events.leave");
        OLD_MESSAGE_KEYS.put("forwarded.spigot.advancement", "events.advancement");
        OLD_MESSAGE_KEYS.put("forwarded.spigot.death", "events.death");
    }

    /** Old command response sub-keys ({@code messages.reply.<sub>}) per command. */
    private static final Map<String, String[]> OLD_COMMAND_REPLIES = new LinkedHashMap<>();

    static {
        OLD_COMMAND_REPLIES.put("ping", new String[] { "message" });
        OLD_COMMAND_REPLIES.put("help", new String[] { "message" });
        OLD_COMMAND_REPLIES.put("reload", new String[] { "message" });
        OLD_COMMAND_REPLIES.put("restart", new String[] { "message" });
        OLD_COMMAND_REPLIES.put("channel", new String[] { "set", "remove", "info", "none" });
        OLD_COMMAND_REPLIES.put("channelset", new String[] { "set", "info", "none" });
        OLD_COMMAND_REPLIES.put("channelremove", new String[] { "remove", "info", "none" });
        OLD_COMMAND_REPLIES.put("channels", new String[] { "list", "list-here" });
        OLD_COMMAND_REPLIES.put("events", new String[] { "add", "remove", "clear", "info", "none" });
    }

    private BotMessageMigration() {
    }

    /** Runs after {@code messages.yml} and {@code messaging.yml} load and before the defaults are registered. */
    public static void run() {
        try {
            Result result = new Result();
            File dataFolder = StreamlineDiscord.getInstance().getDataFolder();
            File backup = new File(dataFolder, "backups" + File.separator + "old-embeds");

            // The old forwarded messages and verification responses.
            Map<String, String> forwarded = moveJsons(new File(dataFolder, "forwarded-jsons"), "forwarded",
                    new File(backup, "forwarded-jsons"), result);
            translateMessages(forwarded, result);

            File commandsFolder = new File(dataFolder, "discord-commands");
            File[] commandFolders = commandsFolder.listFiles(File::isDirectory);
            if (commandFolders != null) {
                for (File commandFolder : commandFolders) {
                    String command = commandFolder.getName();
                    Map<String, String> renamed = moveJsons(commandFolder, command,
                            new File(backup, "discord-commands" + File.separator + command), result);
                    translateCommand(commandFolder, command, renamed, result);
                }
            }

            if (result.isEmpty()) return;

            StreamlineDiscord.getInstance().logInfo("Moved the old Discord message files to the new layout: "
                    + result.embeds.size() + " embed file(s) copied into embeds/" + (result.embeds.isEmpty() ? "" : " (" + String.join(", ", result.embeds) + ")")
                    + ", " + result.settings + " message setting(s) moved into messaging.yml / messages.yml"
                    + (result.backedUp > 0 ? ", " + result.backedUp + " old file(s) kept in backups/old-embeds/." : "."));
        } catch (Exception e) {
            StreamlineDiscord.getInstance().logWarning("Could not move the old Discord message files to the new layout: " + e.getMessage());
            StreamlineDiscord.getInstance().logWarning(e.getStackTrace());
        }
    }

    /**
     * Copies the JSON files of {@code folder} that hold owner changes into {@code embeds/} and moves every JSON
     * file to {@code backupFolder}. Returns old file name -> embed file name for the copies whose name changed.
     */
    private static Map<String, String> moveJsons(File folder, String scope, File backupFolder, Result result) throws Exception {
        Map<String, String> renamed = new LinkedHashMap<>();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
        if (files == null || files.length == 0) {
            deleteIfEmpty(folder, scope);
            return renamed;
        }

        File embeds = Embeds.getFolder();
        embeds.mkdirs();

        for (File file : files) {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JsonElement parsed = parse(text);
            JsonElement shipped = Embeds.readShipped(file.getName());

            // The old loader rewrote shipped files on every start, so a copy matching the shipped default holds no edits.
            boolean edited = shipped == null || parsed == null || ! shipped.equals(parsed);
            if (edited) {
                String target = file.getName();
                File targetFile = new File(embeds, target);
                if (targetFile.exists()) {
                    JsonElement existing = parse(new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8));
                    if (parsed == null || ! parsed.equals(existing)) {
                        target = uniqueName(embeds, scope + "-" + file.getName());
                        targetFile = new File(embeds, target);
                    }
                }
                if (! targetFile.exists()) {
                    Files.write(targetFile.toPath(), text.getBytes(StandardCharsets.UTF_8));
                    result.embeds.add(target);
                }
                if (! target.equals(file.getName())) renamed.put(file.getName(), target);
            }

            backupFolder.mkdirs();
            Files.move(file.toPath(), new File(backupFolder, uniqueName(backupFolder, file.getName())).toPath(), StandardCopyOption.REPLACE_EXISTING);
            result.backedUp++;
        }

        deleteIfEmpty(folder, scope);
        return renamed;
    }

    /** {@code forwarded-jsons/} is only a JSON folder; a command folder keeps its command settings. */
    private static void deleteIfEmpty(File folder, String scope) {
        if (! "forwarded".equals(scope)) return;
        String[] left = folder.list();
        if (left != null && left.length == 0) folder.delete();
    }

    private static void translateMessages(Map<String, String> renamed, Result result) {
        Messages messages = StreamlineDiscord.getMessages();
        Messaging messaging = StreamlineDiscord.getMessaging();

        // Every old messages.yml holds the 'forwarded' section and no new one does, so its presence marks a
        // file written for the old layout, whose plain texts were meant to be sent as text.
        boolean hadForwarded = messages.has("forwarded");
        for (Map.Entry<String, String> entry : OLD_MESSAGE_KEYS.entrySet()) {
            String oldKey = entry.getKey();
            String newKey = entry.getValue();

            String value = messages.has(oldKey) ? messages.getText(oldKey) : null;
            if (value == null) continue;
            boolean sameKey = oldKey.equals(newKey);

            if (value.trim().startsWith("--file:")) {
                messaging.set(newKey, settingFor(value, renamed));
                // The text default is written when the message registers.
                messages.remove(oldKey);
                result.settings++;
            } else if (! sameKey) {
                messages.set(newKey, value);
                messaging.set(newKey, BotMessages.DEFAULT);
                messages.remove(oldKey);
                result.settings++;
            } else if (hadForwarded) {
                messaging.set(newKey, BotMessages.DEFAULT);
                result.settings++;
            }
        }
        if (hadForwarded) messages.remove("forwarded");
    }

    private static void translateCommand(File commandFolder, String command, Map<String, String> renamed, Result result) {
        String[] replies = OLD_COMMAND_REPLIES.get(command);
        File file = new File(commandFolder, command + ".yml");
        if (replies == null || ! file.exists()) return;

        ModularizedConfig config = new ModularizedConfig(StreamlineDiscord.getInstance(), command + ".yml", commandFolder, false) {
            @Override
            public void init() {
            }
        };
        if (! config.getResource().contains("messages")) return;

        Messages messages = StreamlineDiscord.getMessages();
        Messaging messaging = StreamlineDiscord.getMessaging();

        for (String reply : replies) {
            Object raw = config.getResource().get("messages.reply." + reply);
            if (raw == null) continue;
            String value = raw.toString();
            String key = "commands." + command + "." + ("message".equals(reply) ? "response" : reply);

            if (value.trim().startsWith("--file:")) {
                messaging.set(key, settingFor(value, renamed));
            } else {
                messages.set(key, value);
                messaging.set(key, BotMessages.DEFAULT);
            }
            result.settings++;
        }
        config.getResource().remove("messages");
    }

    /** The {@code messaging.yml} value for an old {@code --file:} reference. */
    private static String settingFor(String oldValue, Map<String, String> renamed) {
        String fileName = Embeds.fileName(oldValue);
        String target = renamed.getOrDefault(fileName, fileName);
        return target.toLowerCase(Locale.ROOT).endsWith(".json") ? target.substring(0, target.length() - ".json".length()) : target;
    }

    private static JsonElement parse(String text) {
        try {
            return new JsonParser().parse(text);
        } catch (Exception e) {
            return null;
        }
    }

    private static String uniqueName(File folder, String name) {
        if (! new File(folder, name).exists()) return name;

        String base = name.toLowerCase(Locale.ROOT).endsWith(".json") ? name.substring(0, name.length() - ".json".length()) : name;
        int i = 2;
        while (new File(folder, base + "-" + i + ".json").exists()) i++;
        return base + "-" + i + ".json";
    }

    private static final class Result {
        private final Set<String> embeds = new TreeSet<>();
        private int settings = 0;
        private int backedUp = 0;

        private boolean isEmpty() {
            return embeds.isEmpty() && settings == 0 && backedUp == 0;
        }
    }
}
