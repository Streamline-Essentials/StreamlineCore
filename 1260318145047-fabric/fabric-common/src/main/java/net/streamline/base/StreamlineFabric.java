package net.streamline.base;

import net.streamline.platform.BasePlugin;

public class StreamlineFabric extends BasePlugin {

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

    public static StreamlineFabric getInstance() {
        return (StreamlineFabric) BasePlugin.getInstance();
    }
}
