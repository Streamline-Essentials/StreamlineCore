package net.streamline.base;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.listeners.LegacyClientListener;

/**
 * StreamlineCore for Minecraft Forge 1.8.9, which runs on game clients only: client modules,
 * the HUD and {@code /streamlineclient}. Its metadata is in {@code mcmod.info}.
 */
@Mod(modid = "streamlinecore", useMetadata = true, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class StreamlineLegacyForge extends BasePlugin {

    public StreamlineLegacyForge() {
        initialize();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LegacyClientListener.register();
    }
}
