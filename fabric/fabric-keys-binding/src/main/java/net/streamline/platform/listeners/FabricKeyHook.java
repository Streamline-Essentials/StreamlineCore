package net.streamline.platform.listeners;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.streamline.client.Keybinds;

/**
 * Adds Streamline's key mappings to the Controls screen through Fabric API's
 * {@code KeyBindingHelper}, its name up to Minecraft 1.21.11. Client-only.
 */
public final class FabricKeyHook {

    private FabricKeyHook() {}

    public static void register() {
        for (KeyMapping mapping : Keybinds.all()) KeyBindingHelper.registerKeyBinding(mapping);
    }
}
