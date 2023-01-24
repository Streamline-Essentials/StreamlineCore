package net.streamline.api;

import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.streamline.api.base.module.BaseModule;
import net.streamline.api.base.timers.OneSecondTimer;
import net.streamline.api.base.timers.PlayerExperienceTimer;
import net.streamline.api.base.timers.UserSaveTimer;
import net.streamline.api.command.GivenCommands;
import net.streamline.api.configs.given.CachedUUIDsHandler;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.holders.GeyserHolder;
import net.streamline.api.interfaces.IBackendHandler;
import net.streamline.api.interfaces.IMessenger;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.interfaces.IUserManager;
import net.streamline.api.messages.ProxyMessenger;
import net.streamline.api.messages.proxied.ProxiedMessageManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.profile.StreamlineProfiler;
import net.streamline.api.savables.users.OperatorUser;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.scheduler.ModuleRunnable;
import net.streamline.api.scheduler.ModuleTaskManager;
import net.streamline.api.scheduler.TaskManager;
import net.streamline.api.utils.UserUtils;
import tv.quaint.objects.SingleSet;
import tv.quaint.objects.handling.derived.PluginEventable;
import tv.quaint.storage.resources.databases.DatabaseResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;

public class SLAPI<P extends IStreamline, U extends IUserManager, M extends IMessenger> extends PluginEventable {
    public static class CommandRunner extends BaseRunnable {
        public CommandRunner() {
            super(0, 1);
        }

        @Override
        public void run() {
            List<Integer> toRemove = new ArrayList<>();

            for (int i : getCachedCommands().keySet()) {
                toRemove.add(i);

                SingleSet<String, StreamlineUser> set = getCachedCommands().get(i);
                String command = set.getKey();
                StreamlineUser user = set.getValue();
                command = ModuleUtils.replacePlaceholders(user, command);

                RunType runType = RunType.CONSOLE_COMMAND;
                if (command.startsWith("@ ")) {
                    runType = RunType.NORMAL_COMMAND;
                    command = command.substring(2);
                } else if (command.startsWith("? ")) {
                    runType = RunType.CONSOLE_COMMAND;
                    command = command.substring(3);
                } else if (command.startsWith("! ")) {
                    runType = RunType.OPERATOR_COMMAND;
                    command = command.substring(2);
                } else if (command.startsWith("# ")) {
                    runType = RunType.NORMAL_CHAT;
                    command = command.substring(2);
                }

                switch (runType) {
                    case CONSOLE_COMMAND -> ModuleUtils.runAs(ModuleUtils.getConsole(), command);
                    case NORMAL_COMMAND -> ModuleUtils.runAs(user, command);
                    case OPERATOR_COMMAND -> {
                        OperatorUser operatorUser = new OperatorUser(user);
                        ModuleUtils.runAs(operatorUser, command);
                    }
                    case NORMAL_CHAT -> ModuleUtils.chatAs(user, command);
                }
            }

            for (int i : toRemove) {
                removeCachedCommand(i);
            }
        }
    }

    public enum RunType {
        NORMAL_COMMAND,
        NORMAL_CHAT,

        OPERATOR_COMMAND,

        CONSOLE_COMMAND,
        ;
    }

    @Getter
    private static final String apiChannel = "streamline:api";

    @Getter @Setter
    private static CommandRunner commandRunner;

    @Getter @Setter
    private static LinkedHashMap<Integer, SingleSet<String, StreamlineUser>> cachedCommands = new LinkedHashMap<>();

    public static void addCachedCommand(String command, StreamlineUser user) {
        int lastId = getCachedCommands().keySet().stream().max(Integer::compareTo).orElse(0);
        getCachedCommands().put(lastId + 1, new SingleSet<>(command, user));
    }

    public static void removeCachedCommand(int id) {
        cachedCommands.remove(id);
    }

    @Getter
    private static LuckPerms luckPerms;
    @Getter
    private static GeyserHolder geyserHolder;

    @Getter
    private static File userFolder;
    @Getter
    private static File moduleFolder;
    @Getter
    private static File moduleSaveFolder;
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

    @Getter @Setter
    private static BaseModule baseModule;

    @Getter
    private static OneSecondTimer oneSecondTimer;
    @Getter
    private static PlayerExperienceTimer playerExperienceTimer;
    @Getter
    private static UserSaveTimer userSaveTimer;

    @Getter
    private static TaskManager mainScheduler;
    @Getter
    private static ModuleTaskManager moduleScheduler;

    @Getter @Setter
    private static IBackendHandler backendHandler;

    @Getter @Setter
    private static boolean proxiedServer;
    @Getter @Setter
    private static boolean proxy;

    @Getter @Setter
    private static boolean ready = false;

    @Getter @Setter
    private static DatabaseResource<?> mainDatabase;

    public SLAPI(String identifier, P platform, U userManager, M messenger) {
        super(identifier);
        instance = this;

        this.platform = platform;
        this.userManager = userManager;
        this.messenger = messenger;

        setProxiedServer(platform.getServerType().equals(IStreamline.ServerType.PROXY));
        setProxy(platform.getServerType().equals(IStreamline.ServerType.PROXY));

        userFolder = new File(getDataFolder(), "users" + File.separator);
        moduleFolder = new File(getDataFolder(), "modules" + File.separator);
        moduleSaveFolder = new File(getDataFolder(), "module-resources" + File.separator);
        mainCommandsFolder = new File(getDataFolder(), getCommandsFolderChild());
        userFolder.mkdirs();
        moduleFolder.mkdirs();
        moduleSaveFolder.mkdirs();
        mainCommandsFolder.mkdirs();

        getFiles(getModuleFolder(), file -> {
            if (file.isDirectory()) return true;
            return ! file.getName().endsWith(".jar");
        }).forEach((s, file) -> {
            try {
                Files.move(file.toPath(), Path.of(file.toPath().toString()
                        .replace(getModuleFolder().toPath().toString(), getModuleSaveFolder().toPath().toString())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        mainScheduler = new TaskManager();
        moduleScheduler = new ModuleTaskManager();

        GivenConfigs.init();
        setMainDatabase(GivenConfigs.getMainDatabase());
        GivenCommands.init();

        CachedUUIDsHandler.cachePlayer(GivenConfigs.getMainConfig().userConsoleDiscriminator(), GivenConfigs.getMainConfig().userConsoleNameRegular());
        UserUtils.loadUser(new StreamlineConsole());

        luckPerms = LuckPermsProvider.get();
        geyserHolder = new GeyserHolder();

//        baseModule = new BaseModule();
//        ModuleManager.registerModule(getBaseModule());

        oneSecondTimer = new OneSecondTimer();
        playerExperienceTimer = new PlayerExperienceTimer();
        userSaveTimer = new UserSaveTimer();
        ProxiedMessageManager.init();

        setBaseModule(new BaseModule());

        setCommandRunner(new CommandRunner());

        setReady(true);
    }

    public ConcurrentSkipListMap<String, File> getFiles(File folder, Predicate<File> filePredicate) {
        ConcurrentSkipListMap<String, File> r = new ConcurrentSkipListMap<>();
        if (! folder.isDirectory()) return r;
        File[] files = folder.listFiles();
        if (files == null) return r;

        for (File file : files) {
            if (filePredicate.test(file)) r.put(file.getName(), file);
        }

        return r;
    }

    public InputStream getResourceAsStream(String filename) {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }

    public InputStream getResourceAsStreamMain(String filename) {
        return getPlatform().getMainClassLoader().getResourceAsStream(filename);
    }
}
