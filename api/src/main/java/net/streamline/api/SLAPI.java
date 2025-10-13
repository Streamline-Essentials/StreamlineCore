package net.streamline.api;

import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.streamline.api.base.commands.GivenCommands;
import net.streamline.api.base.timers.OneSecondTimer;
import net.streamline.api.base.timers.UserEnsureTimer;
import net.streamline.api.base.timers.UserSyncTimer;
import net.streamline.api.holders.HolderCompat;
import net.streamline.api.permissions.MetaGrabberImpl;
import singularity.Singularity;
import singularity.interfaces.IMessenger;
import singularity.interfaces.ISingularityExtension;
import singularity.interfaces.IUserManager;
import singularity.interfaces.audiences.IConsoleHolder;
import singularity.interfaces.audiences.IPlayerInterface;
import singularity.modules.CosmicModule;
import singularity.permissions.PermissionUtil;
import singularity.utils.MessageUtils;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SLAPI<C, P extends C, S extends ISingularityExtension, U extends IUserManager<C, P>, M extends IMessenger> extends Singularity<C, P, S, U, M> {
    @Getter @Setter
    private static SLAPI<?, ?, ?, ?, ?> instance;

    @Getter @Setter
    private static Optional<LuckPerms> lpOptional;

    @Getter @Setter
    private static MetaGrabberImpl metaGrabber;

    @Getter
    private static OneSecondTimer oneSecondTimer;
    @Getter
    private static UserSyncTimer userSyncTimer;
    @Getter
    private static UserEnsureTimer userEnsureTimer;

    @Getter
    private static final String slApiChannel = "streamline:api";

    public static boolean isReady() {
        return Singularity.isReady();
    }

    public SLAPI(String identifier, S platform, U userManager, M messenger, IConsoleHolder<C> consoleHolder, IPlayerInterface<P> playerInterface, Supplier<CosmicModule> baseModuleGetter) {
        super(identifier, platform, userManager, messenger, consoleHolder, playerInterface, baseModuleGetter, slApiChannel);
        instance = this;

        lpOptional = Optional.empty();

        metaGrabber = new MetaGrabberImpl();
        PermissionUtil.setMetaGrabber(metaGrabber);

        GivenCommands.init();

        oneSecondTimer = new OneSecondTimer();
        userSyncTimer = new UserSyncTimer();
        userEnsureTimer = new UserEnsureTimer();

        HolderCompat.init();
    }

    public static void onEnable() {
        tryGetLuckPerms();
    }

    public static void onDisable() {
        lpOptional = Optional.empty();
    }

    public static void tryGetLuckPerms() {
        try {
            LuckPerms api = LuckPermsProvider.get();
            lpOptional = Optional.of(api);
        } catch (NoClassDefFoundError ignored) {
            lpOptional = Optional.empty();
        } catch (Exception e) {
            MessageUtils.logInfo("Could not get LuckPerms API...", e);
            lpOptional = Optional.empty();
        }
    }

    public static void withLuckPerms(Consumer<LuckPerms> consumer) {
        tryGetLuckPerms();
        getLpOptional().ifPresent(consumer);
    }
}
