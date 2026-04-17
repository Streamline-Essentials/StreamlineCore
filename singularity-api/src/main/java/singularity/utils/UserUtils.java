package singularity.utils;

import gg.drak.thebase.async.AsyncUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.*;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.*;
import singularity.data.players.location.CosmicLocation;
import singularity.data.teleportation.TPTicket;
import singularity.data.uuid.UuidManager;
import singularity.modules.ModuleUtils;
import singularity.permissions.MetaValue;
import singularity.permissions.PermissionUtil;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Central utility class for managing the lifecycle and lookup of
 * {@link CosmicSender} and {@link CosmicPlayer} instances across all platforms.
 *
 * <p>All currently loaded senders are kept in {@link #loadedSenders}, a concurrent
 * sorted map keyed by UUID string.  The class provides helpers to load, unload,
 * save, delete, and look up senders/players both by UUID and by name, using the
 * {@link singularity.data.uuid.UuidManager} for name-to-UUID resolution.
 *
 * <p>Teleportation requests that need to cross server boundaries are submitted as
 * {@link singularity.data.teleportation.TPTicket}s via the {@code teleport} overloads.
 */
public class UserUtils {

    /**
     * The live registry of all senders (including the console and all online players)
     * that have been loaded into memory, keyed by their UUID string.
     */
    @Getter @Setter
    private static ConcurrentSkipListMap<String, CosmicSender> loadedSenders = new ConcurrentSkipListMap<>();

    /** The singleton console sender; lazily initialised on first access via {@link #getConsole()}. */
    @Setter
    private static CosmicSender console;

    /**
     * Returns the console sender, loading it first if it has not yet been initialised.
     *
     * @return the console {@link CosmicSender}
     */
    public static CosmicSender getConsole() {
        if (! hasConsole()) loadConsole();

        return console;
    }

    /**
     * Ensures the loaded-senders map is initialised and that the console and all
     * currently online platform players are present in it.
     */
    public static void ensureLoadedUsers() {
        if (getLoadedSenders() == null) loadedSenders = new ConcurrentSkipListMap<>();

        if (! hasConsole()) {
            loadConsole();
        }

        ConcurrentSkipListMap<String, CosmicPlayer> ensured = Singularity.getInstance().getUserManager().ensurePlayers();
        loadedSenders.putAll(ensured);
    }

    /**
     * Initialises the console sender if it has not already been created.
     * The newly created sender is stored in {@link #console} but is not added to
     * {@link #loadedSenders}.
     */
    public static void loadConsole() {
        if (! hasConsole()) console = createSender();
    }

    /**
     * Returns {@code true} if the console sender has already been initialised.
     *
     * @return {@code true} when the console field is non-null
     */
    public static boolean hasConsole() {
        return console != null;
    }

    /**
     * Returns a view of {@link #loadedSenders} containing only entries that are
     * instances of {@link CosmicPlayer}.
     *
     * @return a sorted map of UUID to {@link CosmicPlayer} for all loaded players
     */
    public static ConcurrentSkipListMap<String, CosmicPlayer> getLoadedPlayers() {
        ConcurrentSkipListMap<String, CosmicPlayer> r = new ConcurrentSkipListMap<>();

        getLoadedSenders().forEach((s, user) -> {
            if (user instanceof CosmicPlayer) r.put(s, (CosmicPlayer) user);
        });

        return r;
    }

    /**
     * Returns all non-null loaded senders as a sorted set.
     *
     * @return a {@link ConcurrentSkipListSet} of every currently loaded {@link CosmicSender}
     */
    public static ConcurrentSkipListSet<CosmicSender> getLoadedSendersSet() {
        ConcurrentSkipListSet<CosmicSender> r = new ConcurrentSkipListSet<>();

        getLoadedSenders().forEach((s, user) -> {
            if (user != null) r.add(user);
        });

        return r;
    }

    /**
     * Returns all non-null loaded players as a sorted set.
     *
     * @return a {@link ConcurrentSkipListSet} of every currently loaded {@link CosmicPlayer}
     */
    public static ConcurrentSkipListSet<CosmicPlayer> getLoadedPlayersSet() {
        ConcurrentSkipListSet<CosmicPlayer> r = new ConcurrentSkipListSet<>();

        getLoadedPlayers().forEach((s, user) -> {
            if (user != null) r.add(user);
        });

        return r;
    }

    /**
     * Registers a sender in the loaded-senders map, unloading any previous entry for
     * the same UUID, and fires the appropriate load event.
     *
     * @param sender the sender to load; must not be {@code null}
     * @return the same sender instance, for chaining
     */
    public static CosmicSender loadSender(CosmicSender sender) {
        if (isLoaded(sender.getUuid())) unloadSender(sender.getUuid()); // unload the sender if it's already loaded

        getLoadedSenders().put(sender.getUuid(), sender);

        if (sender instanceof CosmicPlayer) new LoadPlayerEvent((CosmicPlayer) sender).fire();
        else ModuleUtils.fireEvent(new LoadSenderEvent(sender));

        return sender;
    }

    /**
     * Removes the given sender from the loaded-senders map and fires the appropriate
     * unload event.
     *
     * @param user the sender to unload; must not be {@code null}
     */
    public static void unloadSender(CosmicSender user) {
        unloadSender(user.getUuid());
    }

    /**
     * Persists the given sender's data to the main database.
     *
     * @param sender the sender to save; a {@code null} value is silently ignored
     * @param async  {@code true} to save asynchronously, {@code false} to save on the
     *               calling thread
     */
    public static void saveSender(CosmicSender sender, boolean async) {
        if (sender == null) return;

        Singularity.getMainDatabase().saveSender(sender, async);
    }

    /**
     * Removes the sender with the given UUID from the loaded-senders map and fires
     * the appropriate unload event.  Does nothing if no sender with that UUID is loaded.
     *
     * @param uuid the UUID string of the sender to unload
     */
    public static void unloadSender(String uuid) {
        CosmicSender sender = getLoadedSenders().remove(uuid);
        if (sender == null) return;

        if (sender instanceof CosmicPlayer) new UnloadPlayerEvent((CosmicPlayer) sender).fire();
        else new UnloadSenderEvent(sender).fire();
    }

    /**
     * Permanently deletes a sender's data from the database and fires the appropriate
     * delete event asynchronously.  If the sender is not currently loaded it is fetched
     * first.
     *
     * @param uuid the UUID string of the sender to delete
     */
    public static void deleteSender(String uuid) {
        CosmicSender sender = getOrGetSender(uuid).orElse(null);
        if (sender == null) return;

        AsyncUtils.executeAsync(() -> {
            Singularity.getMainDatabase().delete(uuid, false);

            if (sender instanceof CosmicPlayer) new DeletePlayerEvent((CosmicPlayer) sender).fire();
            else new DeleteSenderEvent(sender).fire();
        });
    }

    /**
     * Returns {@code true} if a sender with the given UUID is currently in
     * {@link #loadedSenders}.
     *
     * @param uuid the UUID string to check
     * @return {@code true} if the sender is loaded
     */
    public static boolean isLoaded(String uuid) {
        return getSender(uuid).isPresent();
    }

    /**
     * Returns all loaded senders that are currently online.
     *
     * @return a sorted map of UUID to {@link CosmicSender} for every online sender
     */
    public static ConcurrentSkipListMap<String, CosmicSender> getOnlineSenders() {
        ConcurrentSkipListMap<String, CosmicSender> r = new ConcurrentSkipListMap<>();

        getLoadedSenders().forEach((s, user) -> {
            if (user.isOnline()) r.put(user.getUuid(), user);
        });

        return r;
    }

    /**
     * Returns all loaded players that are currently online.
     *
     * @return a sorted map of UUID to {@link CosmicPlayer} for every online player
     */
    public static ConcurrentSkipListMap<String, CosmicPlayer> getOnlinePlayers() {
        ConcurrentSkipListMap<String, CosmicPlayer> r = new ConcurrentSkipListMap<>();

        getLoadedPlayers().forEach((s, user) -> {
            if (user.isOnline()) r.put(user.getUuid(), user);
        });

        return r;
    }

    /**
     * Checks whether a sender record for the given UUID exists in the database.
     * This call blocks until the database query completes.
     *
     * @param uuid the UUID string to query
     * @return {@code true} if the database contains a record for this UUID
     */
    public static boolean userExists(String uuid) {
        return Singularity.getMainDatabase().exists(uuid).join();
    }

    /**
     * Looks up a loaded sender by UUID.  Returns the console sender when the UUID
     * matches the console identifier.  Does NOT load from the database.
     *
     * @param uuid the UUID string to look up; may be {@code null}
     * @return an {@link Optional} containing the loaded sender, or empty if not found
     */
    public static Optional<CosmicSender> getSender(String uuid) {
        if (uuid == null) return Optional.empty();
        if (UuidUtils.isConsole(uuid)) return Optional.of(getConsole());

        CosmicSender sender = getLoadedSenders().get(uuid);
        if (sender == null) return Optional.empty();

        if (sender instanceof CosmicPlayer) {
            CosmicPlayer player = (CosmicPlayer) sender;
            return Optional.of(player);
        }

        return Optional.of(sender);
    }

    /**
     * Looks up a loaded {@link CosmicPlayer} by UUID.  Does NOT load from the database.
     *
     * @param uuid the UUID string of the player to find
     * @return an {@link Optional} containing the loaded player, or empty if not loaded
     *         or the sender is not a player
     */
    public static Optional<CosmicPlayer> getPlayer(String uuid) {
        Optional<CosmicSender> optional = getSender(uuid);
        if (optional.isPresent()) {
            if (optional.get() instanceof CosmicPlayer) return optional.map(s -> (CosmicPlayer) s);
        }

        return Optional.empty();
    }

    /**
     * Registers a {@link CosmicPlayer} via {@link #loadSender(CosmicSender)} and returns it.
     *
     * @param player the player to load; must not be {@code null}
     * @return the same player instance, for chaining
     */
    public static CosmicPlayer loadPlayer(CosmicPlayer player) {
        return (CosmicPlayer) loadSender(player);
    }

    /**
     * Convenience overload that resolves the player for the given sender's UUID via
     * {@link #getOrCreatePlayer(String)}.
     *
     * @param sender the sender whose UUID is used for the lookup
     * @return an {@link Optional} containing the resolved {@link CosmicPlayer}
     */
    public static Optional<CosmicPlayer> getOrCreatePlayer(CosmicSender sender) {
        return getOrCreatePlayer(sender.getUuid());
    }

    /**
     * Creates a new {@link CosmicSender} with no UUID (console placeholder).
     *
     * @return a new default {@link CosmicSender}
     */
    public static CosmicSender createSender() {
        return new CosmicSender();
    }

    /**
     * Creates a new {@link CosmicSender} with the given UUID.
     *
     * @param uuid the UUID string to assign to the new sender
     * @return a new {@link CosmicSender}
     */
    public static CosmicSender createSender(String uuid) {
        return new CosmicSender(uuid);
    }

    /**
     * Creates a new {@link CosmicPlayer} with the given UUID.
     *
     * @param uuid the UUID string to assign to the new player
     * @return a new {@link CosmicPlayer}
     */
    public static CosmicPlayer createPlayer(String uuid) {
        return new CosmicPlayer(uuid);
    }

    /**
     * Returns the sender for the given UUID if already loaded; otherwise creates a
     * new {@link CosmicPlayer}, loads its data from the database, and returns it.
     * Returns empty for the console UUID or invalid player UUIDs.
     *
     * @param uuid the UUID string to resolve
     * @return an {@link Optional} containing the resolved or newly created sender
     */
    public static Optional<CosmicSender> getOrCreateSender(String uuid) {
        Optional<CosmicSender> optional = getOrGetSender(uuid);
        if (optional.isPresent()) return optional;

        if (isConsole(uuid)) return Optional.ofNullable(getConsole());

        if (! UuidUtils.isValidPlayerUUID(uuid)) return Optional.empty();

        CosmicPlayer player = createPlayer(uuid);
        player.load();

        player.augment(Singularity.getMainDatabase().loadPlayer(uuid), false);

        return Optional.of(player);
    }

    /**
     * Returns the player for the given UUID if already loaded; otherwise creates a
     * new {@link CosmicPlayer}, loads its data from the database, and returns it.
     *
     * @param uuid the UUID string of the player to resolve
     * @return an {@link Optional} containing the resolved or newly created player,
     *         or empty if the UUID is invalid
     */
    public static Optional<CosmicPlayer> getOrCreatePlayer(String uuid) {
        Optional<CosmicSender> sender = getOrCreateSender(uuid);
        if (sender.isPresent()) {
            if (sender.get() instanceof CosmicPlayer) {
                return sender.map(s -> (CosmicPlayer) s);
            }
        }

        Optional<CosmicPlayer> optional = getOrGetPlayer(uuid);
        if (optional.isPresent()) return optional;

        if (! UuidUtils.isValidPlayerUUID(uuid)) return Optional.empty();

        CosmicPlayer player = createPlayer(uuid);
        player.load();

        player.augment(Singularity.getMainDatabase().loadPlayer(uuid), false);

        return Optional.of(player);
    }

    /**
     * Creates a temporary (non-persistent) {@link CosmicSender} that will not be saved
     * to the database.
     *
     * @param uuid the UUID string to assign
     * @return a new temporary {@link CosmicSender}
     */
    public static CosmicSender createTemporarySender(String uuid) {
        return new CosmicSender(uuid, true);
    }

    /**
     * Creates a temporary (non-persistent) {@link CosmicPlayer} that will not be saved
     * to the database.
     *
     * @param uuid the UUID string to assign
     * @return a new temporary {@link CosmicPlayer}
     */
    public static CosmicPlayer createTemporaryPlayer(String uuid) {
        return new CosmicPlayer(uuid, true);
    }

    /**
     * Returns the sender for the given UUID if already loaded; otherwise creates a
     * <em>temporary</em> sender whose data is augmented from the database but who is
     * not permanently registered in {@link #loadedSenders}.
     *
     * @param uuid the UUID string to resolve; returns empty when {@code null} or blank
     * @return an {@link Optional} containing the resolved sender
     */
    public static Optional<CosmicSender> getOrGetSender(String uuid) {
        if (uuid == null || uuid.isEmpty()) return Optional.empty();

        if (UuidUtils.isConsole(uuid)) return Optional.of(getConsole());

        Optional<CosmicSender> optional = getSender(uuid);
        if (optional.isPresent()) return optional;

        if (! UuidUtils.isValidPlayerUUID(uuid)) return Optional.empty();

        CosmicPlayer player = createTemporaryPlayer(uuid);
        player.load();

        player.augment(Singularity.getMainDatabase().loadPlayer(uuid), true);

        return Optional.of(player);
    }

    /**
     * Returns the player for the given UUID via {@link #getOrGetSender(String)},
     * casting the result to {@link CosmicPlayer} where applicable.
     *
     * @param uuid the UUID string to resolve
     * @return an {@link Optional} containing the resolved player, or empty if the
     *         sender is not a player
     */
    public static Optional<CosmicPlayer> getOrGetPlayer(String uuid) {
        Optional<CosmicSender> optional = getOrGetSender(uuid);
        if (optional.isPresent()) {
            if (optional.get() instanceof CosmicPlayer) return optional.map(s -> (CosmicPlayer) s);
        }

        return Optional.empty();
    }

    /**
     * Checks whether the given UUID string corresponds to the server console.
     *
     * @param uuid the UUID string to test
     * @return {@code true} if the UUID belongs to the console sender
     */
    public static boolean isConsole(String uuid) {
        if (getConsole() != null) return getConsole().getIdentifier().equals(uuid);

        return UuidUtils.isConsole(uuid);
    }

    /**
     * Returns the sender's online/offline display name by applying the configured
     * online or offline name template via placeholder replacement.
     * Returns a null-indication string if the sender itself is {@code null}.
     *
     * @param stat the sender to format; may be {@code null}
     * @return the formatted online or offline name string
     */
    public static String getOffOnFormatted(CosmicSender stat) {
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat.isOnline()) {
            return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOnlineName());
        } else {
            return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOfflineName());
        }
    }

    /**
     * Returns the sender's online/offline absolute name, equivalent to
     * {@link #getOffOnFormatted(CosmicSender)}.
     * Returns a null-indication string if the sender itself is {@code null}.
     *
     * @param stat the sender to format; may be {@code null}
     * @return the formatted online or offline name string
     */
    public static String getOffOnAbsolute(CosmicSender stat) {
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat.isOnline()) {
            return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOnlineName());
        } else {
            return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOfflineName());
        }
    }

    /**
     * Returns the sender's display name, or a null-indication string if the sender
     * is {@code null}.
     *
     * @param stat the sender to format; may be {@code null}
     * @return the sender's display name
     */
    public static String getFormatted(CosmicSender stat) {
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }
        return stat.getDisplayName();
    }

    /**
     * Returns the sender's current (unformatted) name, or a null-indication string
     * if the sender is {@code null}.
     *
     * @param stat the sender to query; may be {@code null}
     * @return the raw current name of the sender
     */
    public static String getAbsolute(CosmicSender stat) {
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }
        return stat.getCurrentName();
    }

    /**
     * Returns the effective prefix for the given sender.  For players, the prefix is
     * fetched from LuckPerms via {@link singularity.permissions.PermissionUtil}; for
     * non-player senders the meta prefix is used directly.
     *
     * @param user the sender whose prefix to retrieve
     * @return the prefix string, possibly empty
     */
    public static String getPrefix(CosmicSender user) {
        if (! (user instanceof CosmicPlayer)) return user.getMeta().getPrefix();
        CosmicPlayer player = (CosmicPlayer) user;

        Optional<MetaValue> optional = PermissionUtil.getPrefix(player);
        if (optional.isEmpty()) return user.getMeta().getPrefix();

        return optional.get().getValue();
    }

    /**
     * Returns the effective suffix for the given sender.  For players, the suffix is
     * fetched from LuckPerms via {@link singularity.permissions.PermissionUtil}; for
     * non-player senders the meta suffix is used directly.
     *
     * @param user the sender whose suffix to retrieve
     * @return the suffix string, possibly empty
     */
    public static String getSuffix(CosmicSender user) {
        if (! (user instanceof CosmicPlayer)) return user.getMeta().getSuffix();
        CosmicPlayer player = (CosmicPlayer) user;

        Optional<MetaValue> optional = PermissionUtil.getSuffix(player);
        if (optional.isEmpty()) return user.getMeta().getSuffix();

        return optional.get().getValue();
    }

    /**
     * Formats the player's full display name by applying the placeholder replacement
     * engine to the player's configured name template.
     *
     * @param user the player to format
     * @return the fully resolved display name string
     */
    public static String formatName(CosmicPlayer user) {
        return ModuleUtils.replacePlaceholders(user, user.getMeta().getFull());
    }

    /**
     * Returns the player's default formatted nickname by applying placeholder replacement
     * to the full name template.  Equivalent to {@link #formatName(CosmicPlayer)}.
     *
     * @param user the player whose nickname to format
     * @return the fully resolved default nickname
     */
    public static String getFormattedDefaultNickname(CosmicPlayer user) {
        return ModuleUtils.replacePlaceholders(user, user.getMeta().getFull());
    }

    /**
     * Executes the given command as the specified sender, catching any exceptions.
     *
     * @param user    the sender that will run the command
     * @param command the command string to execute (without leading {@code /})
     * @return {@code true} if the command ran without throwing an exception;
     *         {@code false} if an exception was caught
     */
    public static boolean runAs(CosmicSender user, String command) {
        try {
            user.runCommand(command);
            return true;
        } catch (Exception e) {
            MessageUtils.logWarning(e);
            return false;
        }
    }

    /**
     * Resolves a player's UUID string from their username using the
     * {@link singularity.data.uuid.UuidManager}.
     *
     * @param name the player's in-game name
     * @return an {@link Optional} containing the UUID string, or empty if unknown
     */
    public static Optional<String> getUUIDFromName(String name) {
        return UuidManager.getUuidFromName(name);
    }

    /**
     * Resolves a sender by username, looking up the UUID via {@link #getUUIDFromName(String)}
     * and then delegating to {@link #getOrGetSender(String)}.
     *
     * @param name the player's in-game name
     * @return an {@link Optional} containing the resolved sender, or empty if the name
     *         is unknown
     */
    public static Optional<CosmicSender> getOrGetSenderByName(String name) {
        Optional<String> uuid = getUUIDFromName(name);
        if (uuid.isEmpty()) return Optional.empty();

        return getOrGetSender(uuid.get());
    }

    /**
     * Resolves a player by username via {@link #getOrGetSenderByName(String)},
     * returning an empty optional if the sender is not a player.
     *
     * @param name the player's in-game name
     * @return an {@link Optional} containing the resolved player, or empty
     */
    public static Optional<CosmicPlayer> getOrGetPlayerByName(String name) {
        Optional<CosmicSender> sender = getOrGetSenderByName(name);
        if (sender.isEmpty()) return Optional.empty();
        if (sender.get() instanceof CosmicPlayer) {
            return Optional.of((CosmicPlayer) sender.get());
        }

        return Optional.empty();
    }

    /**
     * Resolves or creates a sender by username.  If no loaded entry exists for the
     * given name, a new {@link CosmicSender} is constructed and its data loaded from
     * the database.
     *
     * @param name the player's in-game name
     * @return an {@link Optional} containing the resolved or newly created sender,
     *         or empty if the name is unknown
     */
    public static Optional<CosmicSender> getOrCreateSenderByName(String name) {
        Optional<CosmicSender> optional = getOrGetSenderByName(name);
        if (optional.isPresent()) return optional;

        Optional<String> uuid = getUUIDFromName(name);
        if (uuid.isEmpty()) return Optional.empty();

        CosmicSender sender = createSender(uuid.get());
        sender.load();

        sender.augment(Singularity.getMainDatabase().loadPlayer(uuid.get()), false);

        return Optional.of(sender);
    }

    /**
     * Resolves or creates a player by username.  If no loaded entry exists for the
     * given name, a new {@link CosmicPlayer} is constructed and its data loaded from
     * the database.
     *
     * @param name the player's in-game name
     * @return an {@link Optional} containing the resolved or newly created player,
     *         or empty if the name is unknown
     */
    public static Optional<CosmicPlayer> getOrCreatePlayerByName(String name) {
        Optional<CosmicPlayer> optional = getOrGetPlayerByName(name);
        if (optional.isPresent()) return optional;

        Optional<String> uuid = getUUIDFromName(name);
        if (uuid.isEmpty()) return Optional.empty();

        CosmicPlayer player = createPlayer(uuid.get());
        player.load();

        player.augment(Singularity.getMainDatabase().loadPlayer(uuid.get()), false);

        return Optional.of(player);
    }

    /**
     * Convenience null-returning wrapper for {@link #getOrGetPlayerByName(String)}.
     *
     * @param name the player's in-game name
     * @return the resolved player, or {@code null} if not found
     */
    public static CosmicPlayer getOrGetPlayerByNameNullable(String name) {
        return getOrGetPlayerByName(name).orElse(null);
    }

    /**
     * Convenience null-returning wrapper for {@link #getOrGetSenderByName(String)}.
     *
     * @param name the player's in-game name
     * @return the resolved sender, or {@code null} if not found
     */
    public static CosmicSender getOrGetSenderByNameNullable(String name) {
        return getOrGetSenderByName(name).orElse(null);
    }

    /**
     * Convenience null-returning wrapper for {@link #getOrCreatePlayerByName(String)}.
     *
     * @param name the player's in-game name
     * @return the resolved or newly created player, or {@code null} if the name is unknown
     */
    public static CosmicPlayer getOrCreatePlayerByNameNullable(String name) {
        return getOrCreatePlayerByName(name).orElse(null);
    }

    /**
     * Convenience null-returning wrapper for {@link #getOrCreateSenderByName(String)}.
     *
     * @param name the player's in-game name
     * @return the resolved or newly created sender, or {@code null} if the name is unknown
     */
    public static CosmicSender getOrCreateSenderByNameNullable(String name) {
        return getOrCreateSenderByName(name).orElse(null);
    }

    /**
     * Returns all online players whose current server identifier matches the given name.
     *
     * @param server the server identifier to filter by
     * @return a sorted set of {@link CosmicPlayer}s on the specified server
     */
    public static ConcurrentSkipListSet<CosmicPlayer> getPlayersOn(String server) {
        ConcurrentSkipListSet<CosmicPlayer> r = new ConcurrentSkipListSet<>();

        getOnlinePlayers().forEach((s, user) -> {
            if (user.getServer().getIdentifier().equals(server)) r.add(user);
        });

        return r;
    }

    /**
     * Checks whether the given player is a Geyser (Bedrock edition) player.
     * Detection is based on whether the UUID starts with {@code "0000"}, which is
     * the conventional Geyser prefix.
     *
     * @param user the player to check
     * @return {@code true} if the player is connecting via Geyser
     */
    public static boolean isGeyserPlayer(CosmicPlayer user) {
        return isGeyserPlayer(user.getUuid());
    }

    /**
     * Checks whether the given UUID string belongs to a Geyser (Bedrock edition) player.
     *
     * @param uuid the UUID string to check
     * @return {@code true} if the UUID starts with {@code "0000"}
     */
    public static boolean isGeyserPlayer(String uuid) {
        return uuid.startsWith("0000");
    }

    /**
     * Triggers an asynchronous save for every currently loaded sender.  Useful for
     * periodic data synchronisation or graceful shutdown persistence.
     */
    public static void syncAllUsers() {
        getLoadedSenders().forEach((s, user) -> {
            user.save();
        });
    }

    /**
     * Teleports a sender to another sender, using TPTickets.
     * Should only be called to use a TPTicket for posting, not for direct teleportation.
     * @param sender the sender to teleport
     * @param target the player to teleport to
     */
    public static void teleport(CosmicSender sender, CosmicPlayer target) {
        if (sender.isConsole()) {
            MessageUtils.logWarning("Console attempted to teleport to " + target.getCurrentName());
            return;
        }

        TPTicket ticket = new TPTicket(sender.getIdentifier(), target);
        ticket.post();
    }

    /**
     * Teleports a sender to a location, using TPTickets.
     * Should only be called to use a TPTicket for posting, not for direct teleportation.
     * @param sender the sender to teleport
     * @param location the location to teleport to
     */
    public static void teleport(CosmicSender sender, CosmicLocation location) {
        if (sender.isConsole()) {
            MessageUtils.logWarning("Console attempted to teleport to " + location.asString());
            return;
        }

        TPTicket ticket = new TPTicket(sender.getIdentifier(), location);
        ticket.post();
    }

    /**
     * Returns the current (login) names of all online players as a sorted set.
     *
     * @return a sorted set of player names for every player returned by {@link #getOnlinePlayers()}
     */
    public static ConcurrentSkipListSet<String> getOnlinePlayerNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getOnlinePlayers().forEach((s, user) -> {
            r.add(user.getCurrentName());
        });

        return r;
    }

    /**
     * Returns the UUID strings of all online players as a sorted set.
     *
     * @return a sorted set of UUID strings for every player returned by {@link #getOnlinePlayers()}
     */
    public static ConcurrentSkipListSet<String> getOnlinePlayerUuids() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getOnlinePlayers().forEach((s, user) -> {
            r.add(user.getUuid());
        });

        return r;
    }
}
