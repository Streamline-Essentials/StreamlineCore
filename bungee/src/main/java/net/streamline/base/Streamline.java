package net.streamline.base;

import net.streamline.api.BasePlugin;
import net.streamline.api.modules.ModuleManager;
import net.streamline.base.commands.*;
import net.streamline.base.listeners.BaseListener;
import net.streamline.base.module.BaseModule;
import net.streamline.base.ratapi.StreamlineExpansion;
import net.streamline.base.timers.OneSecondTimer;
import net.streamline.base.timers.PlayerExperienceTimer;
import net.streamline.base.timers.UserSaveTimer;

public class Streamline extends BasePlugin {
    @Override
    public void enable() {
        new StreamlineExpansion();

        new ParseCommand().register();
        new PXPCommand().register();
        new ReloadCommand().register();
        new PTagCommand().register();
        new PointsCommand().register();
        new PlaytimeCommand().register();

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

    }

    @Override
    public void load() {

    }

    @Override
    public void reload() {

    }
}
