package net.streamline.base;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.streamline.platform.BasePlugin;

@Mod("streamlinecore")
public class StreamlineNeoForge extends BasePlugin {

    public StreamlineNeoForge(IEventBus modBus) {
        initialize();
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

    public static StreamlineNeoForge getInstance() {
        return (StreamlineNeoForge) BasePlugin.getInstance();
    }
}
