package net.streamline.base;

import net.streamline.api.command.integrated.*;
import net.streamline.api.modules.ModuleManager;
import net.streamline.base.module.BaseModule;
import net.streamline.base.ratapi.StreamlineExpansion;
import net.streamline.base.timers.OneSecondTimer;
import net.streamline.base.timers.PlayerExperienceTimer;
import net.streamline.base.timers.UserSaveTimer;
import net.streamline.platform.BasePlugin;

public class Streamline extends BasePlugin {
    @Override
    public void enable() {
        new StreamlineExpansion();

        new OneSecondTimer();
        new PlayerExperienceTimer();

        ModuleManager.registerModule(new BaseModule());

        try {
            ModuleManager.registerExternalModules();
            ModuleManager.startModules();
        } catch (Exception e) {
            e.printStackTrace();
        }

        new UserSaveTimer();
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
