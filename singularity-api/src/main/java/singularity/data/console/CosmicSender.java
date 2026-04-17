package singularity.data.console;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.CreatePlayerEvent;
import singularity.data.players.events.CreateSenderEvent;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.meta.SenderMeta;
import singularity.data.players.permissions.SenderPermissions;
import singularity.data.server.CosmicServer;
import singularity.interfaces.audiences.real.RealSender;
import singularity.loading.Loadable;
import singularity.modules.ModuleUtils;
import singularity.text.UsersReplacements;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Base representation of any command sender known to the framework, covering
 * both the server console and online players. Implements {@link Loadable} so
 * that sender data can be asynchronously loaded from and saved to the database.
 *
 * <p>Online players are represented by the subclass {@link CosmicPlayer}, which
 * adds location, IP, and other player-specific state. The console sender is
 * constructed via the no-arg constructor and identified by the configured
 * console discriminator rather than a real UUID.</p>
 */
@Getter
public class CosmicSender implements Loadable<CosmicSender> {

    /**
     * Returns the unique identifier for this sender, delegating to
     * {@link #getUuid()}.
     *
     * @return the sender's UUID string
     */
    public String getIdentifier() {
        return getUuid();
    }

    /**
     * Sets the unique identifier for this sender by delegating to
     * {@link #setUuid(String)}.
     *
     * @param identifier the new UUID string to assign
     */
    public void setIdentifier(String identifier) {
        setUuid(identifier);
    }

    /** The unique identifier string (UUID for players, discriminator for console). */
    @Setter
    private String uuid;

    /** The date/time at which this sender first connected to the network. */
    @Setter
    private Date firstJoinDate;

    /** The date/time of this sender's most recent connection. */
    @Setter
    private Date lastJoinDate;

    /** The date/time at which this sender last disconnected. */
    @Setter
    private Date lastQuitDate;

    /** The current in-game username; updated on join and by {@link #setCurrentNameAsProper()}. */
    private String currentName;

    /** Cumulative number of seconds this sender has been connected. */
    @Setter
    private long playSeconds;

    /** Metadata (nickname, prefix, suffix, tags) associated with this sender. */
    @Setter
    private SenderMeta meta;

    /** Per-sender permission overrides managed by the framework. */
    @Setter
    private SenderPermissions permissions;

    /**
     * Whether this sender has been seen by a proxy-side handler.
     * {@code true} on proxy platforms; set to {@link Singularity#isProxy()} at construction.
     */
    @Setter
    private boolean proxyTouched;

    /**
     * Whether all asynchronous load/augment operations for this sender have
     * completed. Code that requires a fully initialised sender should check
     * this flag.
     */
    @Setter
    private boolean fullyLoaded = false;

    /**
     * When {@code true}, this sender was created as a lightweight temporary
     * placeholder (e.g. for a short lookup) and should not be persisted or
     * announced as a new player.
     */
    @Setter
    private boolean temporary = false;

    /** The placeholder replacements registry scoped to this sender's UUID. */
    @Setter
    private UsersReplacements replacements;

    /**
     * Creates a sender with the given UUID, optionally marking it as temporary.
     * Timestamps are initialised to now, play seconds to zero, and a fresh
     * {@link SenderMeta}, {@link SenderPermissions}, and
     * {@link UsersReplacements} are constructed.
     *
     * @param uuid      the UUID string that identifies this sender
     * @param temporary {@code true} if this sender is a short-lived placeholder
     */
    public CosmicSender(String uuid, boolean temporary) {
        this.uuid = uuid;

        this.temporary = temporary;

        this.firstJoinDate = new Date();
        this.lastJoinDate = new Date();

        this.currentName = "";

        this.playSeconds = 0;

        this.meta = new SenderMeta(this);
        this.permissions = new SenderPermissions(this);

        this.replacements = new UsersReplacements(getUuid());

        this.proxyTouched = Singularity.isProxy();
    }

    /**
     * Creates a non-temporary sender with the given UUID.
     * Delegates to {@link #CosmicSender(String, boolean)}.
     *
     * @param uuid the UUID string that identifies this sender
     */
    public CosmicSender(String uuid) {
        this(uuid, false);
    }

    /**
     * Creates the console sender, using the configured console discriminator as
     * the UUID and the configured console name as the current name. Immediately
     * triggers an asynchronous load from the database via {@link #augment}.
     */
    public CosmicSender() {
        this(
                GivenConfigs.getMainConfig().getConsoleDiscriminator()
        );

        this.setCurrentName(GivenConfigs.getMainConfig().getConsoleName());
        this.augment(Singularity.getMainDatabase().loadPlayer(getIdentifier()), false);
    }

    /**
     * Sets the current username, falling back to the platform's live username
     * lookup when the given value is null, blank, or empty.
     *
     * @param currentName the username to assign; may be null or blank to trigger
     *                    a live lookup
     * @return this instance for chaining
     */
    public CosmicSender setCurrentName(String currentName) {
        String processed = currentName;

        if (processed == null || processed.isBlank() || processed.isEmpty()) {
            processed = Singularity.getInstance().getUserManager().getUsername(getUuid());
        }

        this.currentName = processed;
        return this;
    }

    /**
     * Refreshes the current name by fetching it directly from the platform's
     * user manager, bypassing any cached or stored value.
     */
    public void setCurrentNameAsProper() {
        setCurrentName(Singularity.getInstance().getUserManager().getUsername(getUuid()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(boolean async) {
        UserUtils.saveSender(this, async);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        if (this instanceof CosmicPlayer) {
            UserUtils.loadPlayer((CosmicPlayer) this);
            return;
        }

        UserUtils.loadSender(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unload() {
        UserUtils.unloadSender(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAndUnload(boolean async) {
        save(async);
        unload();
    }

    /**
     * Returns whether this sender is currently tracked in the loaded-senders
     * registry.
     *
     * @return {@code true} if the sender is loaded; {@code false} otherwise
     */
    public boolean isLoaded() {
        return UserUtils.isLoaded(getUuid());
    }

    /**
     * Loads this sender if it is not already present in the loaded-senders
     * registry.
     */
    public void ensureLoaded() {
        if (! isLoaded()) load();
    }

    /**
     * Applies data from a database load future to this sender. Sets
     * {@link #fullyLoaded} to {@code false} until the future completes, then
     * copies all persisted fields from the loaded instance. If no record
     * exists and this is not a get-only lookup, fires the appropriate creation
     * event and saves the sender. On completion sets {@link #fullyLoaded} to
     * {@code true}.
     *
     * @param future a future that resolves to an {@link Optional} containing
     *               the persisted sender data, or empty if none exists
     * @param isGet  {@code true} if this is a read-only lookup; when
     *               {@code false} and no record is found, the sender is
     *               treated as newly created
     * @return this instance for chaining
     */
    @Override
    public CosmicSender augment(CompletableFuture<Optional<CosmicSender>> future, boolean isGet) {
        fullyLoaded = false;

        future.whenComplete((optional, error) -> {
            if (error != null) {
                MessageUtils.logWarning("Failed to augment CosmicSender for UUID: " + getUuid(), error);
                this.fullyLoaded = true;
                return;
            }

            if (optional.isPresent()) {
                CosmicSender sender = optional.get();

                setUuid(sender.getUuid());
                setFirstJoinMillis(sender.getFirstJoinMillis());
                setLastJoinMillis(sender.getLastJoinMillis());
                setLastQuitMillis(sender.getLastQuitMillis());
                setCurrentName(sender.getCurrentName());
                addPlaySeconds(sender.getPlaySeconds());
                setServerName(sender.getServerName());
                setMeta(sender.getMeta());
                setPermissions(sender.getPermissions());

                if (this instanceof CosmicPlayer && sender instanceof CosmicPlayer) augmentMore((CosmicPlayer) sender);

                setCurrentNameAsProper(); // might need to be forced... need to check this...
            } else {
                if (! isGet) {
                    this.temporary = false;
                    if (this instanceof CosmicPlayer) new CreatePlayerEvent((CosmicPlayer) this).fire();
                    else new CreateSenderEvent(this).fire();
                    MessageUtils.logInfo("Created new CosmicPlayer for UUID: " + getUuid() + " (Console: " + isConsole() + ")");

                    this.save();
                } else {
                    unload();
                    fullyLoaded = true;
                    return;
                }
            }

            ensureLoaded();
            fullyLoaded = true;
        });

        return this;
    }

    /**
     * Extension hook called by {@link #augment} when both this sender and the
     * loaded sender are {@link CosmicPlayer} instances. Subclasses may override
     * to copy additional player-specific fields.
     *
     * @param sender the freshly loaded {@link CosmicPlayer} to copy data from
     */
    public void augmentMore(CosmicPlayer sender) {
        // nothing
    }

    /**
     * Returns whether this sender represents the server console (i.e. its UUID
     * equals the configured console discriminator).
     *
     * @return {@code true} if this sender is the console
     */
    public boolean isConsole() {
        return getUuid().equals(getConsoleDiscriminator());
    }

    /**
     * Sends a message to this sender, optionally applying placeholder
     * replacements via {@link ModuleUtils#replacePlaceholders}.
     *
     * @param message the message text to send
     * @param format  {@code true} to expand placeholders; {@code false} to send
     *                the raw string as-is
     */
    public void sendMessage(String message, boolean format) {
        if (format) asReal().sendMessage(ModuleUtils.replacePlaceholders(this, message));
        else asReal().sendMessageRaw(message);
    }

    /**
     * Sends a placeholder-expanded message to this sender.
     * Equivalent to {@code sendMessage(message, true)}.
     *
     * @param message the message text to send (placeholders will be replaced)
     */
    public void sendMessage(String message) {
        sendMessage(message, true);
    }

    /**
     * Executes a command as this sender through the platform's real-sender
     * interface.
     *
     * @param command the command string to execute (without leading slash)
     */
    public void runCommand(String command) {
        asReal().runCommand(command);
    }

    /**
     * Sends a chat message as this sender. Currently a no-op; reserved for
     * future implementation.
     *
     * @param message the chat message to send
     */
    public void chatAs(String message) {
//        asRealConsole().chatAs(message);
    }

    /**
     * Returns whether this sender currently holds the given permission node,
     * as evaluated by the platform's real-sender permission system.
     *
     * @param permission the permission node to test
     * @return {@code true} if the sender has the permission; {@code false} otherwise
     */
    public boolean hasPermission(String permission) {
        return asReal().hasPermission(permission);
    }

    /**
     * Returns whether this sender is considered online. Always {@code true}
     * for {@code CosmicSender}; subclasses may override for offline senders.
     *
     * @return {@code true}
     */
    public boolean isOnline() {
        return true;
    }

    /**
     * Returns the platform-native {@link RealSender} that backs this sender,
     * resolving the console or the correct player instance as appropriate.
     *
     * @return the underlying {@link RealSender} for this sender
     */
    public RealSender<?> asReal() {
        if (this instanceof CosmicPlayer) {
            return ((CosmicPlayer) this).asReal();
        }

        if (getUuid().equals(getConsoleDiscriminator())) {
            return Singularity.getConsole();
        }

        return Singularity.getPlayerFromUuid(this.getUuid());
    }

    /**
     * Returns the {@link CosmicServer} this sender is currently on.
     * For the console this returns a server named by the configured console
     * server value; for players it reads from their current location.
     *
     * @return the current server, or an empty-named server if unavailable
     */
    public CosmicServer getServer() {
        if (isConsole()) return new CosmicServer(getConsoleServer());

        try {
            CosmicPlayer player = (CosmicPlayer) this;
            return player.getLocation().getServer();
        } catch (ClassCastException e) {
            return new CosmicServer("");
        }
    }

    /**
     * Sets the server for this sender. Has no effect if this sender is the
     * console.
     *
     * @param server the {@link CosmicServer} to assign
     */
    public void setServer(CosmicServer server) {
        if (isConsole()) return;

        try {
            CosmicPlayer player = (CosmicPlayer) this;
            player.getLocation().setServer(server);
        } catch (ClassCastException e) {
            // Do nothing
        }
    }

    /**
     * Returns the identifier (name) of the server this sender is currently on.
     *
     * @return the server name string
     */
    public String getServerName() {
        return getServer().getIdentifier();
    }

    /**
     * Sets the server for this sender by name. Has no effect if this sender is
     * the console.
     *
     * @param serverName the name of the server to assign
     */
    public void setServerName(String serverName) {
        if (isConsole()) return;

        try {
            CosmicPlayer player = (CosmicPlayer) this;
            player.getLocation().setServerName(serverName);
        } catch (ClassCastException e) {
            // Do nothing
        }
    }

    /**
     * Returns the console discriminator string from the main configuration.
     *
     * @return the console discriminator (e.g. {@code "%"})
     */
    public static String getConsoleDiscriminator() {
        return GivenConfigs.getMainConfig().getConsoleDiscriminator();
    }

    /**
     * Returns the console display name (with colour codes) from the main
     * configuration.
     *
     * @return the console display name
     */
    public static String getConsoleDisplayName() {
        return GivenConfigs.getMainConfig().getConsoleDisplayName();
    }

    /**
     * Returns the plain console name from the main configuration.
     *
     * @return the console name string
     */
    public static String getConsoleName() {
        return GivenConfigs.getMainConfig().getConsoleName();
    }

    /**
     * Returns the logical server name reported for the console from the main
     * configuration.
     *
     * @return the console server name string
     */
    public static String getConsoleServer() {
        return GivenConfigs.getMainConfig().getConsoleServer();
    }

    /**
     * Sets {@link #firstJoinDate} from an epoch-millisecond value.
     * A value of {@code -1} sets the date to {@code null}.
     *
     * @param millis epoch milliseconds, or {@code -1} to clear the date
     */
    public void setFirstJoinMillis(long millis) {
        if (millis == -1) {
            this.firstJoinDate = null;
            return;
        }
        this.firstJoinDate = new Date(millis);
    }

    /**
     * Sets {@link #lastJoinDate} from an epoch-millisecond value.
     * A value of {@code -1} sets the date to {@code null}.
     *
     * @param millis epoch milliseconds, or {@code -1} to clear the date
     */
    public void setLastJoinMillis(long millis) {
        if (millis == -1) {
            this.lastJoinDate = null;
            return;
        }
        this.lastJoinDate = new Date(millis);
    }

    /**
     * Sets {@link #lastQuitDate} from an epoch-millisecond value.
     * A value of {@code -1} sets the date to {@code null}.
     *
     * @param millis epoch milliseconds, or {@code -1} to clear the date
     */
    public void setLastQuitMillis(long millis) {
        if (millis == -1) {
            this.lastQuitDate = null;
            return;
        }
        this.lastQuitDate = new Date(millis);
    }

    /**
     * Returns the first-join timestamp as epoch milliseconds, or {@code -1}
     * if the date has not been set.
     *
     * @return epoch millis of the first join, or {@code -1}
     */
    public long getFirstJoinMillis() {
        if (this.firstJoinDate == null) return -1;
        return this.firstJoinDate.getTime();
    }

    /**
     * Clears the first-join date, setting it to {@code null}.
     */
    public void setFirstJoinNull() {
        this.firstJoinDate = null;
    }

    /**
     * Returns the last-join timestamp as epoch milliseconds, or {@code -1}
     * if the date has not been set.
     *
     * @return epoch millis of the last join, or {@code -1}
     */
    public long getLastJoinMillis() {
        if (this.lastJoinDate == null) return -1;
        return this.lastJoinDate.getTime();
    }

    /**
     * Clears the last-join date, setting it to {@code null}.
     */
    public void setLastJoinNull() {
        this.lastJoinDate = null;
    }

    /**
     * Returns the last-quit timestamp as epoch milliseconds, or {@code -1}
     * if the date has not been set.
     *
     * @return epoch millis of the last quit, or {@code -1}
     */
    public long getLastQuitMillis() {
        if (this.lastQuitDate == null) return -1;
        return this.lastQuitDate.getTime();
    }

    /**
     * Clears the last-quit date, setting it to {@code null}.
     */
    public void setLastQuitNull() {
        this.lastQuitDate = null;
    }

    /**
     * Builds and returns the formatted display name for this sender. If the
     * meta object has a non-blank full override it is returned as-is; otherwise
     * the console display name is used for the console, and for players the
     * LuckPerms prefix/suffix and nickname (or username fallback) are assembled.
     *
     * @return the fully formatted display name string
     */
    public String getDisplayName() {
        if (! getMeta().getFull().isEmpty() && ! getMeta().getFull().isBlank()) {
            return getMeta().getFull();
        }

        return isConsole() ?
                getConsoleDisplayName() :
                (
                        UserUtils.getPrefix(this) +
                                (
                                        (getMeta().getNickname().isEmpty() || getMeta().getNickname().isBlank()) ?
                                                getCurrentName() :
                                                getMeta().getNickname()
                                ) +
                                UserUtils.getSuffix(this)
                );
    }

    /**
     * Adds the given number of seconds to this sender's cumulative play time.
     *
     * @param amount the number of seconds to add
     */
    public void addPlaySeconds(long amount) {
        this.playSeconds += amount;
    }

    /**
     * Subtracts the given number of seconds from this sender's cumulative play
     * time.
     *
     * @param amount the number of seconds to remove
     */
    public void removePlaySecond(long amount) {
        this.playSeconds -= amount;
    }

    /**
     * Adds the given tag to this sender's meta tag list.
     *
     * @param tag the tag string to add
     */
    public void addTag(String tag) {
        getMeta().addTag(tag);
    }

    /**
     * Removes the given tag from this sender's meta tag list.
     *
     * @param tag the tag string to remove
     */
    public void removeTag(String tag) {
        getMeta().removeTag(tag);
    }

    /**
     * Grants the given permission node to this sender's permission store.
     *
     * @param permission the permission node to add
     */
    public void setPermission(String permission) {
        getPermissions().addPermission(permission);
    }

    /**
     * Revokes the given permission node from this sender's permission store.
     *
     * @param permission the permission node to remove
     */
    public void removePermission(String permission) {
        getPermissions().removePermission(permission);
    }

    /**
     * Reloads this sender's data. No-op in the base class; subclasses may
     * override to re-read data from the database or platform.
     */
    public void reload() {
        // Do nothing
    }

    /**
     * Teleports this sender to the location of the given player via
     * {@link ModuleUtils#teleport(CosmicSender, CosmicPlayer)}.
     *
     * @param player the target player whose location to teleport to
     */
    public void teleport(CosmicPlayer player) {
        ModuleUtils.teleport(this, player);
    }

    /**
     * Teleports this sender to the given location via
     * {@link ModuleUtils#teleport(CosmicSender, CosmicLocation)}.
     *
     * @param location the target location
     */
    public void teleport(CosmicLocation location) {
        ModuleUtils.teleport(this, location);
    }

    /**
     * Connects this sender to the given server. Logs a warning and returns early
     * if {@code server} is {@code null}.
     *
     * @param server the {@link CosmicServer} to connect to
     */
    public void connect(CosmicServer server) {
        if (server == null) {
            MessageUtils.logWarning("Cannot connect to null server for player " + getUuid() + ".");
            return;
        }

        ModuleUtils.connect(this, server);
    }

    /**
     * Connects this sender to the same server as the given sender.
     *
     * @param other the sender whose current server should be used as the target
     */
    public void connect(CosmicSender other) {
        connect(other.getServer());
    }

    /**
     * Connects this sender to the server identified by the given name. Logs a
     * warning and returns early if the name is null or empty.
     *
     * @param serverName the name of the target server
     */
    public void connect(String serverName) {
        if (serverName == null || serverName.isEmpty()) {
            MessageUtils.logWarning("Cannot connect to null server for player " + getUuid() + ".");
            return;
        }

        ModuleUtils.connect(this, serverName);
    }
}
