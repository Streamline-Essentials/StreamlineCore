package net.streamline.base;

import net.streamline.platform.BasePlugin;
import net.streamline.api.modules.ModuleManager;

public class Streamline extends BasePlugin {
    @Override
    public void enable() {
        try {
            ModuleManager.registerExternalModules();
            ModuleManager.startModules();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disable() {
        ModuleManager.stopModules();
    }

    @Override
    public void load() {

    }

    @Override
    public void reload() {

    }
}
