package net.streamline.base.configs;

import de.leonhard.storage.Config;
import net.streamline.base.Streamline;
import net.streamline.api.configs.DatabaseConfig;
import net.streamline.api.configs.FlatFileResource;
import net.streamline.api.configs.StorageUtils;

import java.util.List;

public class MainConfigHandler extends FlatFileResource<Config> {
    public MainConfigHandler() {
        super(Config.class, "main-config.yml", Streamline.getInstance().getDataFolder(), true);
    }

    public StorageUtils.StorageType userUseType() {
        reloadResource();

        return resource.getEnum("users.saving.use", StorageUtils.StorageType.class);
    }

    public String userDatabaseConnectionUri() {
        reloadResource();

        return resource.getString("users.saving.databases.connection-uri");
    }

    public String userDatabaseDatabase() {
        reloadResource();

        return resource.getString("users.saving.databases.database");
    }

    public String userDatabasePrefix() {
        reloadResource();

        return resource.getString("users.saving.databases.prefix");
    }

    public double userCombinedPointsDefault() {
        reloadResource();

        return resource.getDouble("users.combined.points.default");
    }

    public String userConsoleServer() {
        reloadResource();

        return resource.getString("users.console.server");
    }

    public String userConsoleNameRegular() {
        reloadResource();

        return resource.getString("users.console.name.regular");
    }

    public String userConsoleNameFormatted() {
        reloadResource();

        return resource.getString("users.console.name.display");
    }

    public String userConsoleDiscriminator() {
        reloadResource();

        return resource.getString("users.console.discriminator");
    }

    public List<String> userConsoleDefaultTags() {
        reloadResource();

        return resource.getStringList("users.console.tags.default");
    }

    public boolean updatePlayerFormattedNames() {
        reloadResource();

        return resource.getBoolean("users.players.name.formatted");
    }

    public String playerOnlineName() {
        reloadResource();

        return resource.getString("users.players.name.online");
    }

    public String playerOfflineName() {
        reloadResource();

        return resource.getString("users.players.name.offline");
    }

    public boolean announceLevelChangeTitle() {
        reloadResource();

        return resource.getBoolean("users.players.experience.announce.level-change.title");
    }

    public boolean announceLevelChangeChat() {
        reloadResource();

        return resource.getBoolean("users.players.experience.announce.level-change.chat");
    }

    public float playerPayoutExperienceAmount() {
        reloadResource();

        return resource.getFloat("users.players.experience.payout.amount");
    }

    public int playerPayoutExperienceEvery() {
        reloadResource();

        return resource.getInt("users.players.experience.payout.every");
    }

    public int playerStartingLevel() {
        reloadResource();

        return resource.getInt("users.players.experience.starting.level");
    }

    public float playerStartingExperienceAmount() {
        reloadResource();

        return resource.getFloat("users.players.experience.starting.xp");
    }

    public String playerLevelingEquation() {
        reloadResource();

        return resource.getString("users.players.experience.equation");
    }

    public List<String> playerTagsDefault() {
        reloadResource();

        return resource.getStringList("users.players.tags.default");
    }

    public DatabaseConfig getConfiguredDatabase() {
        StorageUtils.DatabaseType databaseType = null;
        if (userUseType().equals(StorageUtils.StorageType.MONGO)) databaseType = StorageUtils.DatabaseType.MONGO;
        if (userUseType().equals(StorageUtils.StorageType.MYSQL)) databaseType = StorageUtils.DatabaseType.MYSQL;
        if (databaseType == null) return null;

        return new DatabaseConfig(userDatabaseConnectionUri(), userDatabaseDatabase(), userDatabasePrefix(), databaseType);
    }
}
