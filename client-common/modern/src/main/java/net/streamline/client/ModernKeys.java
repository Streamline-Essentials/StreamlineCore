package net.streamline.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.streamline.client.compat.ClientCompat;
import singularity.client.input.ClientInput;

/**
 * Key names and held game keys for every modern Minecraft version; {@code ClientCompat}
 * passes its key calls here and reads the window itself, the one part that differs.
 */
public final class ModernKeys {

    private ModernKeys() {}

    /** @return the keyboard key named {@code name}, or {@code null} for none or a mouse button */
    private static InputConstants.Key key(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            InputConstants.Key key = InputConstants.getKey(name);
            // The keyboard type is KEYSYM up to 26.2 and KEYBOARD from 26.3; MOUSE is in both.
            if (key == null || key.getType() == InputConstants.Type.MOUSE || key.getValue() < 0) return null;
            return key;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** @return whether the keyboard key named {@code name} is held now */
    public static boolean down(String name) {
        InputConstants.Key key = key(name);
        return key != null && ClientCompat.keyCodeDown(key.getValue());
    }

    /** @return the key's name as the Controls screen shows it, or {@code null} if it is unknown */
    public static String displayName(String name) {
        InputConstants.Key key = key(name);
        return key == null ? null : key.getDisplayName().getString();
    }

    private static KeyMapping mapping(ClientInput.GameKey key) {
        Options options = Minecraft.getInstance().options;
        if (options == null) return null;
        switch (key) {
            case SPRINT:
                return options.keySprint;
            case SNEAK:
                return options.keyShift;
            default:
                return null;
        }
    }

    /**
     * Presses or releases a game key. With the game's own Toggle option on for it, the
     * mapping flips on every press, so it is pressed only while up and a release that leaves
     * it down is pressed once more to flip it off. A release leaves it down while the player
     * physically holds its key.
     */
    public static void hold(ClientInput.GameKey key, boolean held) {
        KeyMapping mapping = mapping(key);
        if (mapping == null) return;
        if (held) {
            if (! mapping.isDown()) mapping.setDown(true);
            return;
        }
        mapping.setDown(false);
        if (mapping.isDown()) mapping.setDown(true);
        if (! mapping.isDown() && down(mapping.saveString())) mapping.setDown(true);
    }
}
