package net.streamline.base;

import net.streamline.api.BasePlugin;
import net.streamline.api.placeholder.addons.StreamlineExpansion;
import net.streamline.base.commands.PXPCommand;
import net.streamline.base.commands.ParseCommand;
import net.streamline.base.timers.OneSecondTimer;

public class Streamline extends BasePlugin {
    @Override
    public void enable() {
        new StreamlineExpansion();

        new ParseCommand();
        new PXPCommand();

        getScheduler().scheduleAsyncRepeatingTask(null, new OneSecondTimer(), 0, 20);
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
