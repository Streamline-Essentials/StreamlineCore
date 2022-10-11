package net.streamline.base;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.modules.ModuleManager;
import net.streamline.base.runnables.ServerPusher;
import net.streamline.platform.BasePlugin;

public class Streamline extends BasePlugin {
    @Getter @Setter
    private static ServerPusher serverPusher;

    @Override
    public void enable() {
        try {
            ModuleManager.registerExternalModules();
            ModuleManager.startModules();
            setServerPusher(new ServerPusher());
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
