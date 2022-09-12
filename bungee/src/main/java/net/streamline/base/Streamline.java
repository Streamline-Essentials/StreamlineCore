package net.streamline.base;

import net.streamline.api.modules.ModuleManager;
import net.streamline.base.ratapi.StreamlineExpansion;
import net.streamline.platform.BasePlugin;

public class Streamline extends BasePlugin {
    @Override
    public void enable() {
        new StreamlineExpansion();

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
}
