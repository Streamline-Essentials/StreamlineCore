package net.streamline.api;

import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.streamline.api.base.module.BaseModule;
import net.streamline.api.base.timers.CachePurgeTimer;
import net.streamline.api.base.timers.OneSecondTimer;
import net.streamline.api.base.timers.PlayerExperienceTimer;
import net.streamline.api.base.timers.UserSaveTimer;
import net.streamline.api.command.GivenCommands;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.holders.GeyserHolder;
import net.streamline.api.interfaces.IMessenger;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.interfaces.IUserManager;
import net.streamline.api.messages.ProxyMessenger;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.placeholder.RATAPI;
import net.streamline.api.profile.StreamlineProfiler;
import net.streamline.api.scheduler.ModuleTaskManager;
import net.streamline.api.scheduler.TaskManager;

import java.io.File;

public class SLAPI<P extends IStreamline, U extends IUserManager, M extends IMessenger> {
    @Getter
    private static final String apiChannel = "streamline:api";
    @Getter
    private static LuckPerms luckPerms;
    @Getter
    private static GeyserHolder geyserHolder;
    @Getter
    private static RATAPI ratAPI;

    @Getter
    private static File userFolder;
    @Getter
    private static File moduleFolder;
    @Getter
    private static File mainCommandsFolder;
    @Getter
    private static final String commandsFolderChild = "commands" + File.separator;

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
    private static File dataFolder;

    @Getter
    private static BaseModule baseModule;

    @Getter
    private static OneSecondTimer oneSecondTimer;
    @Getter
    private static PlayerExperienceTimer playerExperienceTimer;
    @Getter
    private static UserSaveTimer userSaveTimer;
    @Getter
    private static CachePurgeTimer cachePurgeTimer;

    @Getter
    private static TaskManager mainScheduler;
    @Getter
    private static ModuleTaskManager moduleScheduler;

    public SLAPI(P platform, U userManager, M messenger, File dataFolderExt) {
        instance = this;

        this.platform = platform;
        this.userManager = userManager;
        this.messenger = messenger;

        dataFolder = dataFolderExt;

        userFolder = new File(getDataFolder(), "users" + File.separator);
        moduleFolder = new File(getDataFolder(), "modules" + File.separator);
        mainCommandsFolder = new File(getDataFolder(), getCommandsFolderChild());
        userFolder.mkdirs();
        moduleFolder.mkdirs();
        mainCommandsFolder.mkdirs();

        mainScheduler = new TaskManager();
        moduleScheduler = new ModuleTaskManager();

        GivenConfigs.init();
        GivenCommands.init();

        ratAPI = new RATAPI();

        baseModule = new BaseModule();
        ModuleManager.registerModule(getBaseModule());

        luckPerms = LuckPermsProvider.get();
        geyserHolder = new GeyserHolder();

        oneSecondTimer = new OneSecondTimer();
        playerExperienceTimer = new PlayerExperienceTimer();
        userSaveTimer = new UserSaveTimer();
        cachePurgeTimer = new CachePurgeTimer();
    }
}
