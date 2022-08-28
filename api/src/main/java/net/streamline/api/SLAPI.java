package net.streamline.api;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.command.GivenCommands;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.interfaces.IMessenger;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.interfaces.IUserManager;
import net.streamline.api.messages.ProxyMessenger;
import net.streamline.api.profile.StreamlineProfiler;

import java.io.File;

public class SLAPI<P extends IStreamline, U extends IUserManager, M extends IMessenger> {
    @Getter
    private static final String apiChannel = "streamline:api";

    @Getter
    private static SLAPI<?, ?, ?> instance;
    @Getter
    private final P platform;
    @Getter
    private final U userManager;
    @Getter
    private final M messenger;

    @Getter @Setter
    private ProxyMessenger proxyMessenger;
    @Getter @Setter
    private StreamlineProfiler profiler;

    @Getter
    private final File dataFolder;

    public SLAPI(P platform, U userManager, M messenger, File dataFolder) {
        instance = this;
        this.dataFolder = dataFolder;
        this.platform = platform;
        this.userManager = userManager;
        this.messenger = messenger;

        GivenConfigs.init();
        GivenCommands.init();
    }
}
