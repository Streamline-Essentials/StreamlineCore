package singularity.utils.profiles;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Reads the base64 {@code textures} property of a game profile:
 * {@code {"textures":{"SKIN":{"url":...,"metadata":{"model":"slim"}},"CAPE":{"url":...}}}}.
 */
public final class Textures {
    private Textures() {
    }

    public static Optional<String> skinUrl(String texturesValue) {
        return texture(texturesValue, "SKIN").map(skin -> string(skin, "url"));
    }

    public static Optional<String> capeUrl(String texturesValue) {
        return texture(texturesValue, "CAPE").map(cape -> string(cape, "url"));
    }

    public static boolean isSlim(String texturesValue) {
        return texture(texturesValue, "SKIN")
                .map(skin -> skin.has("metadata") && skin.get("metadata").isJsonObject()
                        ? string(skin.getAsJsonObject("metadata"), "model") : null)
                .map("slim"::equalsIgnoreCase)
                .orElse(false);
    }

    /**
     * A {@code textures} value holding just {@code skinUrl}, unsigned. Player heads accept it;
     * clients only load skins from {@code textures.minecraft.net}.
     */
    public static String forSkinUrl(String skinUrl) {
        JsonObject skin = new JsonObject();
        skin.addProperty("url", skinUrl);
        JsonObject textures = new JsonObject();
        textures.add("SKIN", skin);
        JsonObject root = new JsonObject();
        root.add("textures", textures);
        return Base64.getEncoder().encodeToString(root.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static Optional<JsonObject> texture(String texturesValue, String kind) {
        if (texturesValue == null || texturesValue.isEmpty()) return Optional.empty();
        try {
            String json = new String(Base64.getDecoder().decode(texturesValue.trim()), StandardCharsets.UTF_8);
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            if (! root.has("textures") || ! root.get("textures").isJsonObject()) return Optional.empty();
            JsonElement element = root.getAsJsonObject("textures").get(kind);
            return element != null && element.isJsonObject() ? Optional.of(element.getAsJsonObject()) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }
}
