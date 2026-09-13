package net.streamline.base;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.listeners.PlatformListener;

@Mod("streamlinecore")
public class StreamlineForge extends BasePlugin {

    public StreamlineForge() {
        // Forge instantiates the @Mod class via its constructor
        init();
        MinecraftForge.EVENT_BUS.register(new PlatformListener());
    }

    @Override
    public void load() {
        // Pre-init: nothing needed
    }

    @Override
    public void enable() {
        // Post-init setup
    }

    @Override
    public void disable() {
        // Cleanup
    }

    @Override
    public void reload() {
        // Reload configuration
    }

    public static StreamlineForge getInstance() {
        return (StreamlineForge) BasePlugin.getInstance();
    }
}
