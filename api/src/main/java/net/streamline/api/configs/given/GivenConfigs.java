package net.streamline.api.configs.given;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.whitelist.WhitelistConfig;
import net.streamline.api.punishments.PunishmentConfig;
import net.streamline.api.savables.users.StreamlineUser;
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
    private static PunishmentConfig punishmentConfig;

    @Getter @Setter
    private static CachedUUIDsHandler cachedUUIDsHandler;
    @Getter @Setter
    private static SavedProfileConfig profileConfig;

    @Getter @Setter
    private static File punishmentFolder;

    public static void init() {
        setMainConfig(new MainConfigHandler());
        setMainMessages(new MainMessagesHandler());
        setWhitelistConfig(new WhitelistConfig());
        setPunishmentConfig(new PunishmentConfig());
        setCachedUUIDsHandler(new CachedUUIDsHandler());
        setProfileConfig(new SavedProfileConfig());
    }

    public static void ensureFolders() {
        setPunishmentFolder(new File(SLAPI.getDataFolder(), "punishments" + File.separator));
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
