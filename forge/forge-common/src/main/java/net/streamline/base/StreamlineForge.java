package net.streamline.base;

import net.minecraftforge.fml.common.Mod;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.handlers.GameplayHandler;
import net.streamline.platform.listeners.ForgeListener;

@Mod("streamlinecore")
public class StreamlineForge extends BasePlugin {

    public StreamlineForge() {
        initialize();
        ForgeListener.register();
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.FORGE;
    }

    @Override
    protected GameplayHandler createGameplayHandler() {
        return ForgeListener.gameplayHandler();
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

    public static StreamlineForge getInstance() {
        return (StreamlineForge) BasePlugin.getInstance();
    }
}
