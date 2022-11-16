package net.streamline.api.configs.given;

import net.streamline.api.SLAPI;
import net.streamline.api.configs.StreamlineStorageUtils;
import tv.quaint.storage.StorageUtils;
import tv.quaint.storage.resources.databases.configurations.DatabaseConfig;
import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;
import tv.quaint.thebase.lib.leonhard.storage.sections.FlatFileSection;

import java.util.List;

public class MainConfigHandler extends SimpleConfiguration {
    public MainConfigHandler() {
        super("main-config.yml", SLAPI.getInstance().getDataFolder(), true);
        init();
    }

    public void init() {
        userUseType();
        userDatabaseConnectionUri();
        userDatabaseDatabase();
        userDatabasePrefix();

        userConsoleNameRegular();
        userConsoleDefaultTags();
        userConsoleDiscriminator();
        userConsoleServer();
        userConsoleNameFormatted();

        updatePlayerFormattedNames();

        playerLevelingEquation();
        playerOfflineName();
        playerOnlineName();
        playerTagsDefault();
        playerStartingLevel();
        playerPayoutExperienceAmount();
        playerPayoutExperienceEvery();
        playerStartingExperienceAmount();

        debugNotifyNoModules();

        debugConsoleInfoDisabled();
        debugConsoleInfoPrefix();
        debugConsoleWarningsDisabled();
        debugConsoleWarningsPrefix();
        debugConsoleErrorsDisabled();
        debugConsoleErrorsPrefix();
        debugConsoleDebugDisabled();
        debugConsoleDebugPrefix();

        placeholderCacheReleaseTicks();
        placeholderCacheReleaseInput();
        placeholderCacheReleaseOutput();
    }

    public StorageUtils.SupportedStorageType userUseType() {
        reloadResource();

        return StorageUtils.SupportedStorageType.valueOf(getResource().getOrSetDefault("users.saving.use", StorageUtils.SupportedStorageType.YAML.toString()));
    }

    public String userDatabaseConnectionUri() {
        reloadResource();

        return getResource().getOrSetDefault("users.saving.databases.connection-uri", "mongodb://<user>:<pass>@<host>:<port>/?authSource=admin&readPreference=primary&appname=StreamlineAPI&ssl=false");
    }

    public String userDatabaseDatabase() {
        reloadResource();

        return getResource().getOrSetDefault("users.saving.databases.database", "streamline_users");
    }

    public String userDatabasePrefix() {
        reloadResource();

        return getResource().getOrSetDefault("users.saving.databases.prefix", "sl_");
    }

    public double userCombinedPointsDefault() {
        reloadResource();

        return getResource().getOrSetDefault("users.combined.points.default", 0.0);
    }

    public String userConsoleServer() {
        reloadResource();

        return getResource().getOrSetDefault("users.console.server", "space");
    }

    public String userConsoleNameRegular() {
        reloadResource();

        return getResource().getOrSetDefault("users.console.name.regular", "Console");
    }

    public String userConsoleNameFormatted() {
        reloadResource();

        return getResource().getOrSetDefault("users.console.name.display", "&c&lConsole&r");
    }

    public String userConsoleDiscriminator() {
        reloadResource();

        return getResource().getOrSetDefault("users.console.discriminator", "%");
    }

    public List<String> userConsoleDefaultTags() {
        reloadResource();

        return getResource().getOrSetDefault("users.console.tags.default", List.of("list", "of", "tags"));
    }

    public boolean updatePlayerFormattedNames() {
        reloadResource();

        return getResource().getOrSetDefault("users.players.name.formatted", true);
    }

    public String playerOnlineName() {
        reloadResource();

        return getResource().getOrSetDefault("users.players.name.online", "&d%streamline_user_formatted% &a&l•&r");
    }

    public String playerOfflineName() {
        reloadResource();

        return getResource().getOrSetDefault("users.players.name.offline", "&d%streamline_user_formatted% &c&l•&r");
    }

    public boolean announceLevelChangeTitle() {
        reloadResource();

        return getResource().getOrSetDefault("users.players.experience.announce.level-change.title", true);
    }

    public boolean announceLevelChangeChat() {
        reloadResource();

        return getResource().getOrSetDefault("users.players.experience.announce.level-change.chat", false);
    }

    public double playerPayoutExperienceAmount() {
        reloadResource();

        return getResource().getOrSetDefault("users.players.experience.payout.amount", 1.0);
    }

    public int playerPayoutExperienceEvery() {
        reloadResource();

        return getResource().getOrSetDefault("users.players.experience.payout.every", 400);
    }

    public int playerStartingLevel() {
        reloadResource();

        return getResource().getOrSetDefault("users.players.experience.starting.level", 1);
    }

    public double playerStartingExperienceAmount() {
        reloadResource();

        return getResource().getOrSetDefault("users.players.experience.starting.xp", 0);
    }

    public String playerLevelingEquation() {
        reloadResource();

        return getResource().getOrSetDefault("users.players.experience.equation", "2500 + (2500 * (%streamline_user_level% - 1))");
    }

    public List<String> playerTagsDefault() {
        reloadResource();

        return getResource().getOrSetDefault("users.players.tags.default", List.of("list", "of", "tags"));
    }

    public DatabaseConfig getConfiguredDatabase() {
        FlatFileSection section = getResource().getSection("database");

        StorageUtils.SupportedDatabaseType type = StorageUtils.SupportedDatabaseType.valueOf(section.getOrSetDefault("type", StorageUtils.SupportedDatabaseType.SQLITE.toString()));
        String link;
        switch (type) {
            case MONGO:
                link = section.getOrDefault("link", "mongodb://{{user}}:{{pass}}@{{host}}:{{port}}/{{database}}");
                break;
            case MYSQL:
                link = section.getOrDefault("link", "jdbc:mysql://{{host}}:{{port}}/{{database}}{{options}}");
                break;
            case SQLITE:
                link = section.getOrDefault("link", "jdbc:sqlite:{{database}}.db");
                break;
            default:
                link = section.getOrSetDefault("link", "jdbc:sqlite:{{database}}.db");
                break;
        }
        String host = section.getOrSetDefault("host", "localhost");
        int port = section.getOrSetDefault("port", 3306);
        String username = section.getOrSetDefault("username", "user");
        String password = section.getOrSetDefault("password", "pass1234");
        String database = section.getOrSetDefault("database", "streamline");
        String tablePrefix = section.getOrSetDefault("table-prefix", "sl_");
        String options = section.getOrSetDefault("options", "?useSSL=false&serverTimezone=UTC");

        return new DatabaseConfig(type, link, host, port, username, password, database, tablePrefix, options);
    }

    public boolean debugNotifyNoModules() {
        reloadResource();

        return getResource().getOrSetDefault("debug.notify-on.no-modules", true);
    }

    public boolean debugConsoleInfoDisabled() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.info.full-disable", false);
    }

    public String debugConsoleInfoPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.info.prefix", "&f[&3StreamlineCore&f] &r");
    }

    public boolean debugConsoleWarningsDisabled() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.warnings.full-disable", false);
    }

    public String debugConsoleWarningsPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.warnings.prefix", "&f[&3StreamlineCore&f] &6");
    }

    public boolean debugConsoleErrorsDisabled() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.errors.full-disable", false);
    }

    public String debugConsoleErrorsPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.errors.prefix", "&f[&3StreamlineCore&f] &c");
    }

    public boolean debugConsoleDebugDisabled() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.debug.full-disable", true);
    }

    public String debugConsoleDebugPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("debug.console.debug.prefix", "&f[&3StreamlineCore&f] &f[&cDEBUG&f] &r");
    }

    public int placeholderCacheReleaseTicks() {
        reloadResource();

        return getResource().getOrSetDefault("placeholders.cache.release-after.ticks", 100);
    }

    public String placeholderCacheReleaseInput() {
        reloadResource();

        return getResource().getOrSetDefault("placeholders.cache.release-after.placeholder.input", "{{release}}");
    }

    public String placeholderCacheReleaseOutput() {
        reloadResource();

        return getResource().getOrSetDefault("placeholders.cache.release-after.placeholder.output", "");
    }
}
