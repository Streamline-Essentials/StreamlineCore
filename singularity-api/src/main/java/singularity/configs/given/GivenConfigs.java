package singularity.configs.given;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.whitelist.WhitelistConfig;
import singularity.data.console.CosmicSender;
import singularity.database.ConnectorSet;
import singularity.database.CoreDBOperator;
import singularity.utils.UserUtils;

import java.io.File;

public class GivenConfigs {
    @Getter @Setter
    private static MainConfigHandler mainConfig;
    @Getter @Setter
    private static MainMessagesHandler mainMessages;
    @Getter @Setter
    private static WhitelistConfig whitelistConfig;
    @Getter @Setter
    private static DatabaseConfigHandler databaseConfig;

    @Getter @Setter
    private static File punishmentFolder;

    @Getter @Setter
    private static CoreDBOperator mainDatabase;

    public static void init() {
        setMainConfig(new MainConfigHandler());
        setMainMessages(new MainMessagesHandler());
        setWhitelistConfig(new WhitelistConfig());
        setDatabaseConfig(new DatabaseConfigHandler());

        try {
            ConnectorSet connectorSet = getDatabaseConfig().getConnectorSet();
            CoreDBOperator operator = new CoreDBOperator(connectorSet);
            setMainDatabase(operator);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ensureFolders() {
        setPunishmentFolder(new File(Singularity.getInstance().getDataFolder(), "punishments" + File.separator));
        ensureFolder(getPunishmentFolder());
    }

    public static void ensureFolder(File folder) {
        if (folder == null) return;
        if (! folder.isDirectory()) return;

        folder.mkdirs();
    }

    public static void reloadData() {
        getMainConfig().reloadResource();
        getMainMessages().reloadResource();
        for (CosmicSender user : UserUtils.getLoadedSendersSet()) {
            user.save();
            user.reload();
        }
    }
}
