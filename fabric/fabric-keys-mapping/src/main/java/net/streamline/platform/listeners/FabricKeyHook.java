package net.streamline.platform.listeners;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.streamline.client.Keybinds;

/**
 * Adds Streamline's key mappings to the Controls screen through Fabric API's
 * {@code KeyMappingHelper}, its name from Minecraft 26.1. Client-only.
 */
public final class FabricKeyHook {

    private FabricKeyHook() {}

    public static void register() {
        for (KeyMapping mapping : Keybinds.all()) KeyMappingHelper.registerKeyMapping(mapping);
    }
}
