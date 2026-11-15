package host.plas.discord.data.events.relay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import host.plas.discord.data.events.EventClassifier;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A game event a server without a bot of its own hands to a server with one, the same on every transport.
 *
 * <p>Wire format (JSON):
 * <pre>{@code
 * {"v":1, "id":"<random uuid>", "time":<epoch millis>,
 *  "origin":{"uuid":"<server uuid>", "name":"<event server name>", "core-name":"<Streamline server name>"},
 *  "classifier":"DEATH", "key":"events.death",
 *  "player":{"uuid":"...", "name":"..."},            (player events only)
 *  "placeholders":{"this_death_message":"...", ...}} (the event's own placeholders, already escaped)
 * }</pre>
 * The receiver builds {@code this_player} and {@code this_server*} itself from the raw names, so they are
 * escaped once. Plugin messages carry {@link #toBase64()}: URL-safe Base64 without padding, which never
 * holds the {@code =} or {@code ;} that plugin-message values cannot contain.
 */
@Getter
public final class RelayedEvent {
    public static final int VERSION = 1;

    private final String id;
    private final long time;
    private final String originUuid;
    private final String originName;
    private final String originCoreName;
    private final EventClassifier classifier;
    private final String messageKey;
    /** Null for server events. */
    private final String playerUuid;
    private final String playerName;
    private final Map<String, String> placeholders;

    public RelayedEvent(String id, long time, String originUuid, String originName, String originCoreName,
                        EventClassifier classifier, String messageKey, String playerUuid, String playerName,
                        Map<String, String> placeholders) {
        this.id = id;
        this.time = time;
        this.originUuid = nullToEmpty(originUuid);
        this.originName = nullToEmpty(originName);
        this.originCoreName = nullToEmpty(originCoreName);
        this.classifier = classifier;
        this.messageKey = messageKey;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.placeholders = placeholders == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(placeholders));
    }

    /** A new event from this server, with a fresh id and the current time. */
    public static RelayedEvent create(String originUuid, String originName, String originCoreName,
                                      EventClassifier classifier, String messageKey, String playerUuid, String playerName,
                                      Map<String, String> placeholders) {
        return new RelayedEvent(UUID.randomUUID().toString(), System.currentTimeMillis(), originUuid, originName,
                originCoreName, classifier, messageKey, playerUuid, playerName, placeholders);
    }

    public boolean isPlayerEvent() {
        return playerUuid != null && ! playerUuid.isBlank();
    }

    public String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("v", VERSION);
        root.addProperty("id", id);
        root.addProperty("time", time);

        JsonObject origin = new JsonObject();
        origin.addProperty("uuid", originUuid);
        origin.addProperty("name", originName);
        origin.addProperty("core-name", originCoreName);
        root.add("origin", origin);

        root.addProperty("classifier", classifier.name());
        root.addProperty("key", messageKey);

        if (isPlayerEvent()) {
            JsonObject player = new JsonObject();
            player.addProperty("uuid", playerUuid);
            player.addProperty("name", nullToEmpty(playerName));
            root.add("player", player);
        }

        JsonObject map = new JsonObject();
        placeholders.forEach((key, value) -> {
            if (key != null) map.addProperty(key, nullToEmpty(value));
        });
        root.add("placeholders", map);

        return root.toString();
    }

    /** The event in {@code json}, or null when it is not a relayed event this version understands. */
    public static RelayedEvent fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            // The instance parser, unlike JsonParser.parseString, exists in the older Gson some servers ship.
            @SuppressWarnings("deprecation")
            JsonElement parsed = new JsonParser().parse(json);
            if (! parsed.isJsonObject()) return null;
            JsonObject root = parsed.getAsJsonObject();

            String id = string(root, "id");
            EventClassifier classifier = EventClassifier.parse(string(root, "classifier"));
            String key = string(root, "key");
            if (id == null || classifier == null || key == null) return null;

            long time = root.has("time") ? root.get("time").getAsLong() : 0L;

            JsonObject origin = root.has("origin") && root.get("origin").isJsonObject() ? root.getAsJsonObject("origin") : new JsonObject();
            String playerUuid = null;
            String playerName = null;
            if (root.has("player") && root.get("player").isJsonObject()) {
                JsonObject player = root.getAsJsonObject("player");
                playerUuid = string(player, "uuid");
                playerName = string(player, "name");
            }

            Map<String, String> placeholders = new HashMap<>();
            if (root.has("placeholders") && root.get("placeholders").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("placeholders").entrySet()) {
                    JsonElement value = entry.getValue();
                    placeholders.put(entry.getKey(), value == null || value.isJsonNull() ? "" : value.getAsString());
                }
            }

            return new RelayedEvent(id, time, string(origin, "uuid"), string(origin, "name"), string(origin, "core-name"),
                    classifier, key, playerUuid, playerName, placeholders);
        } catch (Throwable t) {
            return null;
        }
    }

    public String toBase64() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(toJson().getBytes(StandardCharsets.UTF_8));
    }

    public static RelayedEvent fromBase64(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            return fromJson(new String(Base64.getUrlDecoder().decode(encoded.trim()), StandardCharsets.UTF_8));
        } catch (Throwable t) {
            return null;
        }
    }

    private static String string(JsonObject object, String key) {
        if (object == null || ! object.has(key)) return null;
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
