package net.streamline.api.modules;

import tv.quaint.thebase.lib.pf4j.PluginWrapper;

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
