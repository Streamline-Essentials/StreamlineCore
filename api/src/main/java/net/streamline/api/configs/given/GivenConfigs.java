package net.streamline.api.configs.given;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.whitelist.WhitelistConfig;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.database.ConnectorSet;
import net.streamline.api.database.CoreDBOperator;
import net.streamline.api.utils.UserUtils;

import java.io.File;

public class GivenConfigs {
    @Getter @Setter
    private static MainConfigHandler mainConfig;
    @Getter @Setter
    private static MainMessagesHandler mainMessages;
    @Getter @Setter
    private static WhitelistConfig whitelistConfig;

    @Getter @Setter
    private static File punishmentFolder;

    @Getter @Setter
    private static CoreDBOperator mainDatabase;

    public static void init() {
        setMainConfig(new MainConfigHandler());
        setMainMessages(new MainMessagesHandler());
        setWhitelistConfig(new WhitelistConfig());

        try {
            ConnectorSet connectorSet = getMainConfig().getConnectorSet();
            CoreDBOperator operator = new CoreDBOperator(connectorSet);
            setMainDatabase(operator);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ensureFolders() {
        setPunishmentFolder(new File(SLAPI.getInstance().getDataFolder(), "punishments" + File.separator));
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
        for (StreamSender user : UserUtils.getLoadedSendersSet()) {
            user.save();
            user.reload();
        }
    }
}
