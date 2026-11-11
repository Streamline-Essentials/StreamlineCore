package host.plas.discord.messaging;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

/**
 * The optional {@code bot} block of an embed file: {@code bot.avatar.change} with {@code enabled},
 * {@code to-on-message} and {@code to-after-message} (the avatar the bot switches to after sending).
 * Missing or mistyped keys leave the avatar alone.
 */
@Setter
@Getter
public class BotMessageConfig {
    private boolean avatarChange = false;
    private String changeOnMessage = "";
    private String changeAfterMessage = "";

    public BotMessageConfig(JsonObject json) {
        JsonObject change = object(object(object(json, "bot"), "avatar"), "change");
        if (change == null) return;

        avatarChange = bool(change.get("enabled"));
        if (! avatarChange) return;

        changeOnMessage = string(change.get("to-on-message"));
        changeAfterMessage = string(change.get("to-after-message"));
        if (changeAfterMessage.isBlank()) avatarChange = false;
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null) return null;
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static boolean bool(JsonElement element) {
        if (element == null || ! element.isJsonPrimitive()) return false;
        if (element.getAsJsonPrimitive().isBoolean()) return element.getAsBoolean();
        return Boolean.parseBoolean(element.getAsString().trim());
    }

    private static String string(JsonElement element) {
        if (element == null || ! element.isJsonPrimitive()) return "";
        return element.getAsString();
    }
}
