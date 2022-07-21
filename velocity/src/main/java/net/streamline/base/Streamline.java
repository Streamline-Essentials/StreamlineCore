package net.streamline.base;

import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.streamline.api.BasePlugin;
import net.streamline.base.commands.*;
import net.streamline.base.listeners.BaseListener;
import net.streamline.base.ratapi.StreamlineExpansion;
import net.streamline.base.timers.OneSecondTimer;
import net.streamline.base.timers.PlayerExperienceTimer;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.nio.file.Path;

@Plugin(
        id = "streamlineapi",
        name = "StreamlineAPI",
        version = "${project.version}",
        url = "https://github.com/Streamline-Essentials/StreamlineAPI",
        description = "An Essentials plugin for Minecraft server proxies.",
        authors = {
                "Quaint"
        },
        dependencies = {
                @Dependency(id = "luckperms"),
                @Dependency(id = "geyser", optional = true)
        }
)
public class Streamline extends BasePlugin {
    @Inject
    public Streamline(ProxyServer s, Logger l, @DataDirectory Path dd) {
        super(s, l, dd);
    }

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

        new BaseListener.Observer();
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
