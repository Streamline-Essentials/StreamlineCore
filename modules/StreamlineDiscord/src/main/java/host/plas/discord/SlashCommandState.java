package host.plas.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import host.plas.StreamlineDiscord;
import singularity.configs.ModularizedConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * What was last published to Discord as the bot's slash commands, kept in
 * {@code slash-commands.yml} in the module's data folder:
 *
 * <pre>
 * module-version: 1.21.0
 * application-id: '123456789012345678'
 * command-hash: 9f86d081884c7d65...   # SHA-256 of every command's JSON, sorted by name
 * commands:
 *   channel: '123456789012345678'     # slash command name -> Discord command id
 * </pre>
 *
 * When the module version, application id and hash all match what would be sent now,
 * Discord already holds exactly this set and the ids can be reused without asking it.
 * Ids and the application id are stored as strings, since snowflakes do not survive a
 * round trip through YAML numbers.
 */
public class SlashCommandState extends ModularizedConfig {
    public static final String FILE_NAME = "slash-commands.yml";

    public SlashCommandState() {
        super(StreamlineDiscord.getInstance(), FILE_NAME, false);
    }

    @Override
    public void init() {
    }

    public static String moduleVersion() {
        try {
            return StreamlineDiscord.getInstance().getWrapper().getDescriptor().getVersion();
        } catch (Throwable t) {
            return "unknown";
        }
    }

    /**
     * The command ids stored for this exact version, hash and application, or {@code null}
     * when anything differs, an id is missing for one of {@code names}, or the file was
     * marked stale.
     */
    public Map<String, Long> idsIfCurrent(String version, String hash, long applicationId, Collection<String> names) {
        reloadResource(true);

        if (! version.equals(getResource().getString("module-version"))) return null;
        if (! String.valueOf(applicationId).equals(getResource().getString("application-id"))) return null;
        String storedHash = getResource().getString("command-hash");
        if (storedHash == null || storedHash.isEmpty() || ! storedHash.equals(hash)) return null;

        Map<String, Long> ids = new LinkedHashMap<>();
        for (String name : names) {
            String raw = getResource().getString("commands." + name);
            if (raw == null) return null;
            try {
                ids.put(name, Long.parseLong(raw.trim()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return ids;
    }

    public void save(String version, String hash, long applicationId, Map<String, Long> ids) {
        reloadResource(true);

        getResource().removeAll("commands");
        write("module-version", version);
        write("application-id", String.valueOf(applicationId));
        write("command-hash", hash);
        ids.forEach((name, id) -> write("commands." + name, String.valueOf(id)));
    }

    /** Makes the next start publish to Discord again, whatever the stored hash said. */
    public void markStale() {
        reloadResource(true);
        write("command-hash", "");
    }

    /**
     * A SHA-256 over each command's JSON in the given order, with object keys sorted so
     * the result does not depend on map iteration order. Arrays keep their order, since
     * the order of options and choices is part of what Discord shows.
     */
    public static String hash(List<String> commandJsons) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String json : commandJsons) {
                digest.update(canonical(new JsonParser().parse(json)).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash slash commands", e);
        }
    }

    private static String canonical(JsonElement element) {
        if (element == null || element.isJsonNull()) return "null";
        if (element.isJsonPrimitive()) return element.toString();
        if (element.isJsonArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonElement e : (JsonArray) element) parts.add(canonical(e));
            return "[" + String.join(",", parts) + "]";
        }

        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : ((JsonObject) element).entrySet()) {
            sorted.put(entry.getKey(), canonical(entry.getValue()));
        }
        List<String> parts = new ArrayList<>();
        sorted.forEach((key, value) -> parts.add(new com.google.gson.JsonPrimitive(key) + ":" + value));
        return "{" + String.join(",", parts) + "}";
    }
}
