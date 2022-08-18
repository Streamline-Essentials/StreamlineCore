package net.streamline.api.configs.given;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.whitelist.WhitelistConfig;
import net.streamline.api.punishments.PunishmentConfig;

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
    private static File punishmentFolder;

    public static void init() {
        setMainConfig(new MainConfigHandler());
        setMainMessages(new MainMessagesHandler());
        setWhitelistConfig(new WhitelistConfig());
        setPunishmentConfig(new PunishmentConfig());
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
}
