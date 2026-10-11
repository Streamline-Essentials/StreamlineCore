package net.streamline.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.streamline.client.compat.ClientCompat;
import net.streamline.client.compat.ClientKeys;
import singularity.client.ClientBridge;

import java.util.Arrays;
import java.util.List;

/**
 * Streamline's key mappings, listed under StreamlineClient in the Controls screen, where the
 * player rebinds them (the game keeps them in {@code options.txt}): HUD Editor, Right Shift by
 * default, and StreamlineClient Menu, unbound by default. Each loader registers {@link #all()}
 * its own way and calls {@link #tick()} every client tick.
 */
public final class Keybinds {

    /** {@code KEY_RSHIFT} is a GLFW code up to 26.2 and an SDL one from 26.3; each version compiles its own. */
    public static final KeyMapping HUD_EDITOR = ClientKeys.mapping("key.streamlinecore.hud_editor", InputConstants.KEY_RSHIFT);
    public static final KeyMapping MENU = ClientKeys.mapping("key.streamlinecore.menu", InputConstants.UNKNOWN.getValue());

    private Keybinds() {}

    public static List<KeyMapping> all() {
        return Arrays.asList(HUD_EDITOR, MENU);
    }

    /**
     * Opens the screen of a key pressed since the last tick. The game counts presses only
     * while no screen is open, so these never stack on top of another screen.
     */
    public static void tick() {
        boolean editor = drain(HUD_EDITOR);
        boolean menu = drain(MENU);
        if (ClientCompat.currentScreen() != null) return;
        if (editor) ClientBridge.openHudEditor();
        else if (menu) ClientBridge.openMenu();
    }

    private static boolean drain(KeyMapping mapping) {
        boolean pressed = false;
        while (mapping.consumeClick()) pressed = true;
        return pressed;
    }
}
