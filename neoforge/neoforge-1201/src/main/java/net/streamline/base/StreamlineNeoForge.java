package net.streamline.base;

import net.minecraftforge.fml.common.Mod;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.listeners.ForgeListener;

/**
 * NeoForge for 1.20.1 forked from Forge before the package rename, so it is driven by the
 * Forge EventBus 6 listener under {@code net.minecraftforge}.
 */
@Mod("streamlinecore")
public class StreamlineNeoForge extends BasePlugin {

    public StreamlineNeoForge() {
        initialize();
        ForgeListener.register();
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.NEOFORGE;
    }

    @Override
    public void load() {
    }

    @Override
    public void enable() {
    }

    @Override
    public void disable() {
    }

    @Override
    public void reload() {
    }

    public static StreamlineNeoForge getInstance() {
        return (StreamlineNeoForge) BasePlugin.getInstance();
    }
}
