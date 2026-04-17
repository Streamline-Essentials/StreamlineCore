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

/**
 * The main API entry point for the Streamline plugin framework.
 *
 * <p>{@code SLAPI} extends {@link Singularity} with Streamline-specific
 * integrations including LuckPerms for permission/meta resolution, periodic
 * background timers for user synchronisation and integrity checks, and the
 * plugin messaging channel used for cross-server communication.
 *
 * <p>Type parameters mirror those of {@link Singularity}:
 * <ul>
 *   <li>{@code C} — the command-sender type for this platform</li>
 *   <li>{@code P} — the player type (must extend {@code C})</li>
 *   <li>{@code S} — the platform extension implementation</li>
 *   <li>{@code U} — the user-manager implementation</li>
 *   <li>{@code M} — the messenger implementation</li>
 * </ul>
 *
 * @param <C> command-sender type
 * @param <P> player type
 * @param <S> platform plugin implementation type
 * @param <U> user manager type
 * @param <M> messenger type
 */
public class SLAPI<C, P extends C, S extends ISingularityExtension, U extends IUserManager<C, P>, M extends IMessenger> extends Singularity<C, P, S, U, M> {

    /** The currently active {@code SLAPI} instance. */
    @Getter @Setter
    private static SLAPI<?, ?, ?, ?, ?> instance;

    /**
     * An {@link Optional} wrapping the LuckPerms API, or empty when LuckPerms
     * is not installed or has failed to load.
     */
    @Getter @Setter
    private static Optional<LuckPerms> lpOptional;

    /**
     * The {@link MetaGrabberImpl} used to resolve chat-meta (prefix/suffix)
     * values from LuckPerms for players.
     */
    @Getter @Setter
    private static MetaGrabberImpl metaGrabber;

    /** Timer that fires every server tick for sub-second scheduling needs. */
    @Getter
    private static OneSecondTimer oneSecondTimer;

    /** Timer that periodically persists all loaded users to storage. */
    @Getter
    private static UserSyncTimer userSyncTimer;

    /** Timer that periodically verifies the integrity of all loaded users. */
    @Getter
    private static UserEnsureTimer userEnsureTimer;

    /**
     * The plugin-messaging channel identifier used by Streamline for
     * cross-server communication ({@code "streamline:api"}).
     */
    @Getter
    private static final String slApiChannel = "streamline:api";

    /**
     * Returns whether the underlying {@link Singularity} framework has
     * finished initialising and is ready to serve requests.
     *
     * @return {@code true} if the framework is ready, {@code false} otherwise
     */
    public static boolean isReady() {
        return Singularity.isReady();
    }

    /**
     * Constructs a new {@code SLAPI} instance for the given platform and
     * registers it as the global singleton. Initialises LuckPerms, the
     * permission meta-grabber, built-in commands, background timers, and
     * dependency-holder compatibility.
     *
     * @param identifier       unique string identifier for this plugin instance
     * @param platform         the platform-specific extension implementation
     * @param userManager      the user-manager to use for player handling
     * @param messenger        the messenger used for sending chat messages
     * @param consoleHolder    holder wrapping the console command sender
     * @param playerInterface  interface for platform-native player operations
     * @param baseModuleGetter supplier that returns the base {@link CosmicModule}
     */
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

    /**
     * Called when the platform plugin enables. Attempts to resolve the
     * LuckPerms API so it is available for the session.
     */
    public static void onEnable() {
        tryGetLuckPerms();
    }

    /**
     * Called when the platform plugin disables. Clears the cached LuckPerms
     * reference to avoid stale state across reloads.
     */
    public static void onDisable() {
        lpOptional = Optional.empty();
    }

    /**
     * Attempts to resolve the LuckPerms API via {@link LuckPermsProvider#get()}
     * and stores the result in {@link #lpOptional}. Sets the optional to empty
     * when the LuckPerms class is absent ({@link NoClassDefFoundError}) or any
     * other exception occurs during resolution.
     */
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

    /**
     * Resolves the LuckPerms API if necessary and then passes it to the
     * supplied consumer. The consumer is only invoked when LuckPerms is
     * available.
     *
     * @param consumer the action to perform with the {@link LuckPerms} instance
     */
    public static void withLuckPerms(Consumer<LuckPerms> consumer) {
        tryGetLuckPerms();
        getLpOptional().ifPresent(consumer);
    }
}
