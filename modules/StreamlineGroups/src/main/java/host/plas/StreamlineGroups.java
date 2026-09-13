package host.plas;

import host.plas.commands.GCCommand;
import host.plas.commands.GroupChatCommand;
import host.plas.commands.GuildCommand;
import host.plas.commands.PCCommand;
import host.plas.commands.PartyCommand;
import host.plas.configs.Configs;
import host.plas.configs.DefaultRoles;
import host.plas.configs.Messages;
import host.plas.data.Guild;
import host.plas.database.GuildKeeper;
import host.plas.database.GuildLoader;
import host.plas.database.PlayerKeeper;
import host.plas.database.PlayerLoader;
import host.plas.listeners.MainListener;
import host.plas.placeholders.GroupsExpansion;
import host.plas.data.GroupManager;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.modules.ModuleUtils;
import singularity.modules.SimpleModule;

import java.io.File;

public class StreamlineGroups extends SimpleModule {
    @Getter @Setter
    private static StreamlineGroups instance;

    @Getter @Setter
    private static File usersFolder;
    @Getter @Setter
    private static File groupsFolder;

    @Getter @Setter
    private static Configs configs;
    @Getter @Setter
    private static Messages messages;
    @Getter @Setter
    private static DefaultRoles defaultRoles;

    @Getter @Setter
    private static MainListener mainListener;

    @Getter @Setter
    private static GroupsExpansion groupsExpansion;

    @Getter @Setter
    private static PlayerKeeper playerKeeper;
    @Getter @Setter
    private static PlayerLoader playerLoader;

    @Getter @Setter
    private static GuildKeeper guildKeeper;
    @Getter @Setter
    private static GuildLoader guildLoader;

    public StreamlineGroups(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onLoad() {
        instance = this;
        groupsExpansion = new GroupsExpansion();
    }

    @Override
    public void onEnable() {
        configs = new Configs();
        messages = new Messages();
        defaultRoles = new DefaultRoles();

        usersFolder = new File(getDataFolder(), "users" + File.separator);
        groupsFolder = new File(getDataFolder(), "groups" + File.separator);
        usersFolder.mkdirs();
        groupsFolder.mkdirs();

        mainListener = new MainListener();
        ModuleUtils.listen(mainListener, this);

        // Keepers and loaders come up before the commands and listener that use them.
        playerKeeper = new PlayerKeeper();
        playerLoader = new PlayerLoader();
        guildKeeper = new GuildKeeper();
        guildLoader = new GuildLoader();

        loadStoredGuilds();

        getGroupsExpansion().init();

        new PartyCommand(this).register();
        new PCCommand().register();
        new GuildCommand(this).register();
        new GCCommand().register();
        new GroupChatCommand().register();
    }

    /**
     * Brings every stored guild into memory, registering each with both the guild loader
     * and the group manager so that {@code GroupManager.getGuild} can find it.
     *
     * <p>Runs off-thread: the module does not need guilds present to finish enabling, and
     * the query would otherwise block server startup.</p>
     */
    private void loadStoredGuilds() {
        guildKeeper.pullAllGuilds().whenComplete((guilds, throwable) -> {
            if (throwable != null) {
                logWarning("Failed to load stored guilds: " + throwable.getMessage());
                return;
            }

            guilds.forEach(guild -> {
                guildLoader.load(guild);
                guild.load();
            });

            logInfo("Loaded " + guilds.size() + " guild(s).");
        });
    }

    /**
     * Parties are session-scoped, so they are disbanded; guilds are persistent, so they are
     * saved and unloaded rather than torn down. Every loaded player's chat routing is
     * flushed on the way out.
     */
    @Override
    public void onDisable() {
        GroupManager.getLoadedParties().forEach(group -> {
            try {
                if (group instanceof Guild) {
                    ((Guild) group).save(false);
                } else {
                    group.disband();
                }
            } catch (Throwable e) {
                logWarning("Failed to shut down group " + group.getClassedIdentifier() + ": " + e.getMessage());
            }
        });

        GroupManager.getLoadedParties().clear();

        if (playerLoader != null) {
            playerLoader.getLoaded().forEach(player -> {
                try {
                    player.save(false);
                } catch (Throwable e) {
                    logWarning("Failed to save grouped player " + player.getUuid() + ": " + e.getMessage());
                }
            });
            playerLoader.getLoaded().clear();
        }

        if (guildLoader != null) guildLoader.getLoaded().clear();

        getGroupsExpansion().stop();
    }
}
