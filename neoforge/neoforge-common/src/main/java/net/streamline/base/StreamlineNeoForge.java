package net.streamline.base;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.listeners.NeoForgeListener;

@Mod("streamlinecore")
public class StreamlineNeoForge extends BasePlugin {

    public StreamlineNeoForge(IEventBus modBus) {
        initialize();
        NeoForge.EVENT_BUS.register(new NeoForgeListener());
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
