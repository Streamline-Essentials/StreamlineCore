package host.plas;

import host.plas.database.Keeper;
import host.plas.database.MyLoader;
import host.plas.essentials.users.UtilitiesUser;
import lombok.Getter;
import lombok.Setter;
import singularity.modules.ModuleUtils;
import singularity.modules.SimpleModule;
import host.plas.commands.*;
import host.plas.configs.*;
import host.plas.executables.ExecutableHandler;
import host.plas.executables.aliases.AliasGetter;
import host.plas.listeners.MainListener;
import host.plas.ratapi.UtilitiesExpansion;
import org.pf4j.PluginWrapper;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class StreamlineUtilities extends SimpleModule {
    @Getter
    static StreamlineUtilities instance;
    @Getter
    static File executablesFolder;
    @Getter
    static File functionsFolder;
    @Getter
    static File scriptsFolder;
    @Getter
    static File aliasesFolder;
    @Getter
    static CustomPlaceholdersConfig customPlaceholdersConfig;
    @Getter
    static ServersConfig serversConfig;
    @Getter
    static GroupedPermissionConfig groupedPermissionConfig;
    @Getter
    static MaintenanceConfig maintenanceConfig;
    @Getter
    static Configs configs;
    @Getter
    static Messages messages;
    @Getter
    static MainListener mainListener;

    @Getter
    static UtilitiesExpansion utilitiesExpansion;

    @Getter
    static ConcurrentSkipListSet<AliasGetter> getters;

    @Getter @Setter
    static File usersFolder;

    @Getter @Setter
    private static Keeper keeper;

    public StreamlineUtilities(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onLoad() {
        instance = this;
        setKeeper(new Keeper());

        getters = new ConcurrentSkipListSet<>(List.of(new AliasGetter("servers", ModuleUtils::getServerNames),
                new AliasGetter("online_names", () -> {
                    ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

                    ModuleUtils.getOnlineUsers().forEach((a, b) -> {
                        if (b.isOnline()) r.add(b.getCurrentName());
                    });

                    return r;
                }),
                new AliasGetter("online_uuids", () -> {
                    ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

                    ModuleUtils.getOnlineUsers().forEach((a, b) -> {
                        if (b.isOnline()) r.add(b.getUuid());
                    });

                    return r;
                }),
                new AliasGetter("loaded_names", () -> {
                    ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

                    ModuleUtils.getLoadedSendersSet().forEach(a -> {
                        r.add(a.getCurrentName());
                    });

                    return r;
                }),
                new AliasGetter("loaded_uuids", () -> {
                    ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

                    ModuleUtils.getLoadedSendersSet().forEach(a -> {
                        r.add(a.getUuid());
                    });

                    return r;
                })));

        getGetters().forEach(a -> {
            ExecutableHandler.unloadGetter(a);
            ExecutableHandler.loadGetter(a);
        });
        utilitiesExpansion = new UtilitiesExpansion();

        new FunctionCommand().register();
        new BroadcastCommand().register();
        new TextCommand().register();
        new TitleCommand().register();
        new OnlineCommand().register();
        new TeleportCommand().register();
        new TeleportHereCommand().register();
        new MaintenanceCommand().register();
        new WhitelistCommand().register();
        new NickCommand().register();
        new SudoCommand().register();
        new SudoOpCommand().register();
        new TPACommand().register();
        new TPAHereCommand().register();
        new ConnectCommand().register();
        new ModifyServerCommand().register();
        new DeleteHomeCommand().register();
        new SetHomeCommand().register();
        new HomeCommand().register();
    }

    @Override
    public void onEnable() {
        executablesFolder = new File(getDataFolder(), "executables" + File.separator);
        executablesFolder.mkdirs();
        functionsFolder = new File(getExecutablesFolder(), "functions" + File.separator);
        if (functionsFolder.mkdirs()) {
            String dstring = "default-function.sf";
            File file = new File(functionsFolder, dstring);
            try (InputStream in = getResourceAsStream(dstring)) {
                assert in != null;
                Files.copy(in, file.toPath());
                logInfo("Set up default file: " + dstring);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        scriptsFolder = new File(getExecutablesFolder(), "scripts" + File.separator);
        scriptsFolder.mkdirs();
        aliasesFolder = new File(getExecutablesFolder(), "aliases" + File.separator);
        if (aliasesFolder.mkdirs()) {
            String hubstring = "hub-alias.yml";
            File hfile = new File(aliasesFolder, hubstring);
            try (InputStream in = getResourceAsStream(hubstring)) {
                assert in != null;
                Files.copy(in, hfile.toPath());
                logInfo("Set up default file: " + hubstring);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String lobbystring = "lobby-alias.yml";
            File lfile = new File(aliasesFolder, lobbystring);
            try (InputStream in = getResourceAsStream(lobbystring)) {
                assert in != null;
                Files.copy(in, lfile.toPath());
                logInfo("Set up default file: " + lobbystring);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ExecutableHandler.loadFunctions(functionsFolder);
        ExecutableHandler.enableAllFunctions();
        ExecutableHandler.loadAllAliases(aliasesFolder);

        customPlaceholdersConfig = new CustomPlaceholdersConfig();
        serversConfig = new ServersConfig();
        serversConfig.reloadResource(true);
        groupedPermissionConfig = new GroupedPermissionConfig();
        maintenanceConfig = new MaintenanceConfig();

        configs = new Configs();
        messages = new Messages();

        mainListener = new MainListener();
        ModuleUtils.listen(mainListener, this);
    }

    @Override
    public void onDisable() {
        MyLoader.getInstance().getLoaded().forEach(UtilitiesUser::unregister); // saves as well (built in)

        ExecutableHandler.unloadAllAliases();
        ExecutableHandler.disableAllFunctions();
        ExecutableHandler.unloadAllFunctions();

        getUtilitiesExpansion().stop();
    }
}
