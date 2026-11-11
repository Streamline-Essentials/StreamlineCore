package host.plas.discord.messaging;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import host.plas.StreamlineDiscord;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/**
 * The {@code embeds/} folder in the module's data folder: one Discord embed per {@code .json} file.
 * Shipped defaults live in the module jar under {@code embeds/} and are copied out only when the
 * file is missing, so an owner's edits are never overwritten. Files are read on every use, so an
 * edit applies to the next message without a reload.
 */
public final class Embeds {
    /** Prefix of the shipped default embeds inside the module jar. */
    public static final String RESOURCE_FOLDER = "embeds/";

    private Embeds() {
    }

    public static File getFolder() {
        return new File(StreamlineDiscord.getInstance().getDataFolder(), "embeds");
    }

    /**
     * The file name an owner's setting refers to: {@code join}, {@code join.json} and
     * {@code --file:join.json} all name {@code join.json}.
     */
    public static String fileName(String setting) {
        String name = setting.trim();
        if (name.startsWith("--file:")) name = name.substring("--file:".length()).trim();
        if (! name.toLowerCase(Locale.ROOT).endsWith(".json")) name = name + ".json";
        return name;
    }

    public static File getFile(String setting) {
        return new File(getFolder(), fileName(setting));
    }

    /** Copies the shipped embed of this name into {@code embeds/} unless a file of that name exists. */
    public static void ensureShipped(String setting) {
        String name = fileName(setting);
        File target = new File(getFolder(), name);
        if (target.exists()) return;

        try (InputStream in = openShipped(name)) {
            if (in == null) return;
            getFolder().mkdirs();
            Files.copy(in, target.toPath());
        } catch (Exception e) {
            StreamlineDiscord.getInstance().logWarning("Could not write the default embed '" + name + "': " + e.getMessage());
        }
    }

    /** The shipped default embed of this name, or null when the jar holds none. */
    public static JsonElement readShipped(String setting) {
        try (InputStream in = openShipped(fileName(setting))) {
            if (in == null) return null;
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return new JsonParser().parse(reader);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Reads an embed file. Returns null and logs why when the file is missing or is not a JSON
     * object, so callers can fall back to the plain text.
     */
    public static JsonObject read(String setting) {
        File file = getFile(setting);
        if (! file.exists()) {
            ensureShipped(setting);
            if (! file.exists()) {
                StreamlineDiscord.getInstance().logWarning("The embed file 'embeds/" + file.getName() + "' does not exist.");
                return null;
            }
        }

        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JsonElement element = new JsonParser().parse(text);
            if (! element.isJsonObject()) {
                StreamlineDiscord.getInstance().logWarning("The embed file 'embeds/" + file.getName() + "' must hold a JSON object ({ ... }).");
                return null;
            }
            return element.getAsJsonObject();
        } catch (Exception e) {
            StreamlineDiscord.getInstance().logWarning("The embed file 'embeds/" + file.getName() + "' is not valid JSON: " + e.getMessage());
            return null;
        }
    }

    private static InputStream openShipped(String name) {
        ClassLoader loader = StreamlineDiscord.getInstance().getWrapper().getPluginClassLoader();
        return loader.getResourceAsStream(RESOURCE_FOLDER + name);
    }
}
