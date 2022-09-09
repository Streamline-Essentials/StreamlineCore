package net.streamline.api;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.base.module.BaseModule;
import net.streamline.api.base.timers.CachePurgeTimer;
import net.streamline.api.base.timers.OneSecondTimer;
import net.streamline.api.base.timers.PlayerExperienceTimer;
import net.streamline.api.base.timers.UserSaveTimer;
import net.streamline.api.command.GivenCommands;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.interfaces.IMessenger;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.interfaces.IUserManager;
import net.streamline.api.messages.ProxyMessenger;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.profile.StreamlineProfiler;
import net.streamline.api.scheduler.ModuleTaskManager;
import net.streamline.api.scheduler.TaskManager;

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

    @Getter
    private final BaseModule baseModule;

    @Getter
    private final OneSecondTimer oneSecondTimer;
    @Getter
    private final PlayerExperienceTimer playerExperienceTimer;
    @Getter
    private final UserSaveTimer userSaveTimer;
    @Getter
    private final CachePurgeTimer cachePurgeTimer;

    @Getter @Setter
    private TaskManager mainScheduler;
    @Getter @Setter
    private ModuleTaskManager moduleScheduler;

    public SLAPI(P platform, U userManager, M messenger, File dataFolder) {
        instance = this;
        this.dataFolder = dataFolder;
        this.platform = platform;
        this.userManager = userManager;
        this.messenger = messenger;

        mainScheduler = new TaskManager();
        moduleScheduler = new ModuleTaskManager();

        GivenConfigs.init();
        GivenCommands.init();

        baseModule = new BaseModule();
        ModuleManager.registerModule(getBaseModule());

        oneSecondTimer = new OneSecondTimer();
        playerExperienceTimer = new PlayerExperienceTimer();
        userSaveTimer = new UserSaveTimer();
        cachePurgeTimer = new CachePurgeTimer();
    }
}
