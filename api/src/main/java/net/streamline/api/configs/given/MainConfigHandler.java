package net.streamline.api.configs.given;

import de.leonhard.storage.Config;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.DatabaseConfig;
import net.streamline.api.configs.FlatFileResource;
import net.streamline.api.configs.StorageUtils;

import java.util.List;

public class MainConfigHandler extends FlatFileResource<Config> {
    public MainConfigHandler() {
        super(Config.class, "main-config.yml", SLAPI.getInstance().getDataFolder(), true);
    }

    public StorageUtils.StorageType userUseType() {
        reloadResource();

        return StorageUtils.StorageType.valueOf(resource.getOrSetDefault("users.saving.use", StorageUtils.StorageType.YAML.toString()));
    }

    public String userDatabaseConnectionUri() {
        reloadResource();

        return resource.getOrSetDefault("users.saving.databases.connection-uri", "mongodb://<user>:<pass>@<host>:<port>/?authSource=admin&readPreference=primary&appname=StreamlineAPI&ssl=false");
    }

    public String userDatabaseDatabase() {
        reloadResource();

        return resource.getOrSetDefault("users.saving.databases.database", "streamline_users");
    }

    public String userDatabasePrefix() {
        reloadResource();

        return resource.getOrSetDefault("users.saving.databases.prefix", "sl_");
    }

    public double userCombinedPointsDefault() {
        reloadResource();

        return resource.getOrSetDefault("users.combined.points.default", 0.0);
    }

    public String userConsoleServer() {
        reloadResource();

        return resource.getOrSetDefault("users.console.server", "space");
    }

    public String userConsoleNameRegular() {
        reloadResource();

        return resource.getOrSetDefault("users.console.name.regular", "Console");
    }

    public String userConsoleNameFormatted() {
        reloadResource();

        return resource.getOrSetDefault("users.console.name.display", "&c&lConsole&r");
    }

    public String userConsoleDiscriminator() {
        reloadResource();

        return resource.getOrSetDefault("users.console.discriminator", "%");
    }

    public List<String> userConsoleDefaultTags() {
        reloadResource();

        return resource.getOrSetDefault("users.console.tags.default", List.of("list", "of", "tags"));
    }

    public boolean updatePlayerFormattedNames() {
        reloadResource();

        return resource.getOrSetDefault("users.players.name.formatted", true);
    }

    public String playerOnlineName() {
        reloadResource();

        return resource.getOrSetDefault("users.players.name.online", "&d%streamline_user_formatted% &a&l•&r");
    }

    public String playerOfflineName() {
        reloadResource();

        return resource.getOrSetDefault("users.players.name.offline", "&d%streamline_user_formatted% &c&l•&r");
    }

    public boolean announceLevelChangeTitle() {
        reloadResource();

        return resource.getOrSetDefault("users.players.experience.announce.level-change.title", true);
    }

    public boolean announceLevelChangeChat() {
        reloadResource();

        return resource.getOrSetDefault("users.players.experience.announce.level-change.chat", false);
    }

    public double playerPayoutExperienceAmount() {
        reloadResource();

        return resource.getOrSetDefault("users.players.experience.payout.amount", 1.0);
    }

    public int playerPayoutExperienceEvery() {
        reloadResource();

        return resource.getOrSetDefault("users.players.experience.payout.every", 400);
    }

    public int playerStartingLevel() {
        reloadResource();

        return resource.getOrSetDefault("users.players.experience.starting.level", 1);
    }

    public double playerStartingExperienceAmount() {
        reloadResource();

        return resource.getOrSetDefault("users.players.experience.starting.xp", 0);
    }

    public String playerLevelingEquation() {
        reloadResource();

        return resource.getOrSetDefault("users.players.experience.equation", "2500 + (2500 * (%streamline_user_level% - 1))");
    }

    public List<String> playerTagsDefault() {
        reloadResource();

        return resource.getOrSetDefault("users.players.tags.default", List.of("list", "of", "tags"));
    }

    public DatabaseConfig getConfiguredDatabase() {
        StorageUtils.DatabaseType databaseType = null;
        if (userUseType().equals(StorageUtils.StorageType.MONGO)) databaseType = StorageUtils.DatabaseType.MONGO;
        if (userUseType().equals(StorageUtils.StorageType.MYSQL)) databaseType = StorageUtils.DatabaseType.MYSQL;
        if (databaseType == null) return null;

        return new DatabaseConfig(userDatabaseConnectionUri(), userDatabaseDatabase(), userDatabasePrefix(), databaseType);
    }

    public boolean debugNotifyNoModules() {
        reloadResource();

        return resource.getOrSetDefault("debug.notify-on.no-modules", true);
    }
}
