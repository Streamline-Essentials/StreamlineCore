package net.streamline.api.modules;

import org.pf4j.PluginWrapper;

public abstract class SimpleModule extends StreamlineModule {

    public SimpleModule(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    @Override
    public void registerCommands() {

    }
}
