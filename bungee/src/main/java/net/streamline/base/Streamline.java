package net.streamline.base;

import net.streamline.api.BasePlugin;
import net.streamline.base.commands.*;
import net.streamline.base.ratapi.StreamlineExpansion;
import net.streamline.base.timers.OneSecondTimer;
import net.streamline.base.timers.PlayerExperienceTimer;

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
