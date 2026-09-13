package net.streamline.base;

import net.streamline.platform.BasePlugin;

public class StreamlineFabric extends BasePlugin {

    @Override
    public void load() {}

    @Override
    public void enable() {}

    @Override
    public void disable() {}

    @Override
    public void reload() {}

    public static StreamlineFabric getInstance() {
        return (StreamlineFabric) BasePlugin.getInstance();
    }
}
