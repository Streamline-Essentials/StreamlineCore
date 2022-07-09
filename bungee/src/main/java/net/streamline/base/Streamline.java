package net.streamline.base;

import net.streamline.api.BasePlugin;
import net.streamline.api.placeholder.addons.StreamlineExpansion;
import net.streamline.base.commands.PXPCommand;
import net.streamline.base.commands.ParseCommand;
import net.streamline.base.timers.OneSecondTimer;
import net.streamline.base.timers.PlayerExperienceTimer;

public class Streamline extends BasePlugin {
    @Override
    public void enable() {
        new StreamlineExpansion();

        new ParseCommand();
        new PXPCommand();

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
