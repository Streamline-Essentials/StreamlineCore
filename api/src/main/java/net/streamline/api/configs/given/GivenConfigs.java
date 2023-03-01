package net.streamline.api.configs.given;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.whitelist.WhitelistConfig;
import net.streamline.api.punishments.PunishmentConfig;
import net.streamline.api.savables.MongoMainResource;
import net.streamline.api.savables.MySQLMainResource;
import net.streamline.api.savables.SQLiteMainResource;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;
import tv.quaint.storage.resources.databases.DatabaseResource;

import java.io.File;

public class GivenConfigs {
    @Getter @Setter
    private static MainConfigHandler mainConfig;
    @Getter @Setter
    private static MainMessagesHandler mainMessages;
    @Getter @Setter
    private static WhitelistConfig whitelistConfig;
    @Getter @Setter
    private static PunishmentConfig punishmentConfig;

    @Getter @Setter
    private static CachedUUIDsHandler cachedUUIDsHandler;
    @Getter @Setter
    private static SavedProfileConfig profileConfig;

    @Getter @Setter
    private static File punishmentFolder;

    @Getter @Setter
    private static DatabaseResource<?> mainDatabase;

    public static void init() {
        setMainConfig(new MainConfigHandler());
        setMainMessages(new MainMessagesHandler());
        setWhitelistConfig(new WhitelistConfig());
        setPunishmentConfig(new PunishmentConfig());
        setCachedUUIDsHandler(new CachedUUIDsHandler());
        setProfileConfig(new SavedProfileConfig());

        switch (getMainConfig().savingUseType()) {
            case MONGO:
                setMainDatabase(new MongoMainResource(getMainConfig().getConfiguredDatabase()));
                break;
            case MYSQL:
                setMainDatabase(new MySQLMainResource(getMainConfig().getConfiguredDatabase()));
                break;
            case SQLITE:
                setMainDatabase(new SQLiteMainResource(getMainConfig().getConfiguredDatabase()));
                break;
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
        for (StreamlineUser user : UserUtils.getLoadedUsersSet()) {
            user.saveAll();
            user.reload();
        }
    }
}
