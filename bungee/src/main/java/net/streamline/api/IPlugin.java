package net.streamline.api;

import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.streamline.api.command.CommandException;
import net.streamline.api.command.CommandSender;
import net.streamline.api.command.ConsoleCommandSender;
import net.streamline.api.command.ModuleCommand;
import net.streamline.api.entities.IPlayer;
import net.streamline.api.help.HelpMap;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ServicesManager;
import net.streamline.api.permissions.Permissible;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.scheduler.StreamlineScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * Represents a plugin implementation.
 */
public interface IPlugin {

    /**
     * Used for all administrative messages, such as an operator using a
     * command.
     * <p>
     * For use in {@link #broadcast(java.lang.String, java.lang.String)}.
     */
    public static final String BROADCAST_CHANNEL_ADMINISTRATIVE = "bukkit.broadcast.admin";

    /**
     * Used for all announcement messages, such as informing users that a
     * player has joined.
     * <p>
     * For use in {@link #broadcast(java.lang.String, java.lang.String)}.
     */
    public static final String BROADCAST_CHANNEL_USERS = "bukkit.broadcast.user";

    /**
     * Gets the name of this plugin implementation.
     *
     * @return name of this plugin implementation
     */
    @NotNull
    public String getName();

    /**
     * Gets the version string of this plugin implementation.
     *
     * @return version of this plugin implementation
     */
    @NotNull
    public String getVersion();

    /**
     * Gets a view of all currently logged in players. This {@linkplain
     * Collections#unmodifiableCollection(Collection) view} is a reused
     * object, making some operations like {@link Collection#size()}
     * zero-allocation.
     * <p>
     * The collection is a view backed by the internal representation, such
     * that, changes to the internal state of the plugin will be reflected
     * immediately. However, the reuse of the returned collection (identity)
     * is not strictly guaranteed for future or all implementations. Casting
     * the collection, or relying on interface implementations (like {@link
     * Serializable} or {@link List}), is deprecated.
     * <p>
     * For safe consequential iteration or mimicking the old array behavior,
     * using {@link Collection#toArray(Object[])} is recommended. For making
     * snapshots, {@link ImmutableList#copyOf(Collection)} is recommended.
     *
     * @return a view of currently online players.
     */
    @NotNull
    public Collection<? extends IPlayer> getOnlinePlayers();

    /**
     * Get the maximum amount of players which can login to this plugin.
     *
     * @return the amount of players this plugin allows
     */
    public int getMaxPlayers();

    /**
     * Gets the plugin resource pack uri, or empty string if not specified.
     *
     * @return the plugin resource pack uri, otherwise empty string
     */
    @NotNull
    public String getResourcePack();

    /**
     * Gets the SHA-1 digest of the plugin resource pack, or empty string if
     * not specified.
     *
     * @return the SHA-1 digest of the plugin resource pack, otherwise empty
     *     string
     */
    @NotNull
    public String getResourcePackHash();

    /**
     * Gets the custom prompt message to be shown when the plugin resource
     * pack is required, or empty string if not specified.
     *
     * @return the custom prompt message to be shown when the plugin resource,
     *     otherwise empty string
     */
    @NotNull
    public String getResourcePackPrompt();

    /**
     * Gets whether the plugin resource pack is enforced.
     *
     * @return whether the plugin resource pack is enforced
     */
    public boolean isResourcePackRequired();

    /**
     * Gets whether this plugin has a whitelist or not.
     *
     * @return whether this plugin has a whitelist or not
     */
    public boolean hasWhitelist();

    /**
     * Sets if the plugin is whitelisted.
     *
     * @param value true for whitelist on, false for off
     */
    public void setWhitelist(boolean value);

    /**
     * Gets whether the plugin whitelist is enforced.
     *
     * If the whitelist is enforced, non-whitelisted players will be
     * disconnected when the plugin whitelist is reloaded.
     *
     * @return whether the plugin whitelist is enforced
     */
    public boolean isWhitelistEnforced();

    /**
     * Sets if the plugin whitelist is enforced.
     *
     * If the whitelist is enforced, non-whitelisted players will be
     * disconnected when the plugin whitelist is reloaded.
     *
     * @param value true for enforced, false for not
     */
    public void setWhitelistEnforced(boolean value);

    /**
     * Gets a list of whitelisted players.
     *
     * @return a set containing all whitelisted players
     */
    @NotNull
    public Set<SavablePlayer> getWhitelistedPlayers();

    /**
     * Reloads the whitelist from disk.
     */
    public void reloadWhitelist();

    /**
     * Broadcast a message to all players.
     * <p>
     * This is the same as calling {@link #broadcast(java.lang.String,
     * java.lang.String)} to {@link #BROADCAST_CHANNEL_USERS}
     *
     * @param message the message
     * @return the number of players
     */
    public int broadcastMessage(@NotNull String message);

    /**
     * Gets the name of the update folder. The update folder is used to safely
     * update plugins at the right moment on a plugin load.
     * <p>
     * The update folder name is relative to the plugins folder.
     *
     * @return the name of the update folder
     */
    @NotNull
    public String getUpdateFolder();

    /**
     * Gets the update folder. The update folder is used to safely update
     * plugins at the right moment on a plugin load.
     *
     * @return the update folder
     */
    @NotNull
    public File getUpdateFolderFile();

    /**
     * Gets the value of the connection throttle setting.
     *
     * @return the value of the connection throttle setting
     */
    public long getConnectionThrottle();

    /**
     * Gets a player object by the given username.
     * <p>
     * This method may not return objects for offline players.
     *
     * @param name the name to look up
     * @return a player if one was found, null otherwise
     */
    @Nullable
    public ProxiedPlayer getPlayer(@NotNull String name);

    /**
     * Gets the player with the exact given name, case insensitive.
     *
     * @param name Exact name of the player to retrieve
     * @return a player object if one was found, null otherwise
     */
    @Nullable
    public ProxiedPlayer getPlayerExact(@NotNull String name);

    /**
     * Attempts to match any players with the given name, and returns a list
     * of all possibly matches.
     * <p>
     * This list is not sorted in any particular order. If an exact match is
     * found, the returned list will only contain a single result.
     *
     * @param name the (partial) name to match
     * @return list of all possible players
     */
    @NotNull
    public List<ProxiedPlayer> matchPlayer(@NotNull String name);

    /**
     * Gets the player with the given UUID.
     *
     * @param id UUID of the player to retrieve
     * @return a player object if one was found, null otherwise
     */
    @Nullable
    public ProxiedPlayer getPlayer(@NotNull UUID id);

    /**
     * Gets the plugin manager for interfacing with plugins.
     *
     * @return a plugin manager for this Plugin instance
     */
    @NotNull
    public ModuleManager getModuleManager();

    /**
     * Gets the scheduler for managing scheduled events.
     *
     * @return a scheduling service for this module
     */
    @NotNull
    public StreamlineScheduler getScheduler();

    /**
     * Gets a services manager.
     *
     * @return s services manager
     */
    @NotNull
    public ServicesManager getServicesManager();

    /**
     * Gets a list of all servers on this plugin.
     *
     * @return a list of servers
     */
    @NotNull
    public List<ServerInfo> getServers();

//    /**
//     * Creates or loads a server with the given name using the specified
//     * options.
//     * <p>
//     * If the server is already loaded, it will just return the equivalent of
//     * getServer(creator.name()).
//     *
//     * @param creator the options to use when creating the server
//     * @return newly created or loaded server
//     */
//    @Nullable
//    public Server createServer(@NotNull ServerCreator creator);

    /**
     * Unloads a server with the given name.
     *
     * @param name Name of the server to unload
     * @param save whether to save the chunks before unloading
     * @return true if successful, false otherwise
     */
    public boolean unloadServer(@NotNull String name, boolean save);

    /**
     * Unloads the given server.
     *
     * @param server the server to unload
     * @param save whether to save the chunks before unloading
     * @return true if successful, false otherwise
     */
    public boolean unloadServer(@NotNull Server server, boolean save);

    /**
     * Gets the server with the given name.
     *
     * @param name the name of the server to retrieve
     * @return a server with the given name, or null if none exists
     */
    @Nullable
    public ServerInfo getServer(@NotNull String name);

    /**
     * Gets the server from the given Unique ID.
     *
     * @param uid a unique-id of the server to retrieve
     * @return a server with the given Unique ID, or null if none exists
     */
    @Nullable
    public Server getServer(@NotNull UUID uid);

    /**
     * Reloads the plugin, refreshing settings and plugin information.
     */
    public void reload();

    /**
     * Reload only the Minecraft data for the plugin. This includes custom
     * advancements and loot tables.
     */
    public void reloadData();

    /**
     * Gets a {@link ModuleCommand} with the given name or alias.
     *
     * @param name the name of the command to retrieve
     * @return a plugin command if found, null otherwise
     */
    @Nullable
    public ModuleCommand getModuleCommand(@NotNull String name);

    /**
     * Writes loaded players to disk.
     */
    public void savePlayers();

    /**
     * Dispatches a command on this plugin, and executes it if found.
     *
     * @param sender the apparent sender of the command
     * @param commandLine the command + arguments. Example: <code>test abc
     *     123</code>
     * @return returns false if no target is found
     * @throws CommandException thrown when the executor for the given command
     *     fails with an unhandled exception
     */
    public boolean dispatchCommand(@NotNull CommandSender sender, @NotNull String commandLine) throws CommandException;

    /**
     * Gets a list of command aliases defined in the plugin properties.
     *
     * @return a map of aliases to command names
     */
    @NotNull
    public Map<String, String[]> getCommandAliases();

    /**
     * Gets whether the plugin should send a preview of the player's chat
     * message to the client when the player types a message
     *
     * @return true if the plugin should send a preview, false otherwise
     */
    public boolean shouldSendChatPreviews();

    /**
     * Gets whether the plugin only allow players with Mojang-signed public key
     * to join
     *
     * @return true if only Mojang-signed players can join, false otherwise
     */
    public boolean isEnforcingSecureProfiles();

    /**
     * Gets whether the Plugin hide online players in plugin status.
     *
     * @return true if the plugin hide online players, false otherwise
     */
    public boolean getHideOnlinePlayers();

    /**
     * Gets whether the Plugin is in online mode or not.
     *
     * @return true if the plugin authenticates clients, false otherwise
     */
    public boolean getOnlineMode();

    /**
     * Shutdowns the plugin, stopping everything.
     */
    public void shutdown();

    /**
     * Broadcasts the specified message to every user with the given
     * permission name.
     *
     * @param message message to broadcast
     * @param permission the required permission {@link Permissible
     *     permissibles} must have to receive the broadcast
     * @return number of message recipients
     */
    public int broadcast(@NotNull String message, @NotNull String permission);

    /**
     * Gets the player by the given name, regardless if they are offline or
     * online.
     * <p>
     * This method may involve a blocking web request to get the UUID for the
     * given name.
     * <p>
     * This will return an object even if the player does not exist. To this
     * method, all players will exist.
     *
     * @param name the name the player to retrieve
     * @return an offline player
     * @see #getOfflinePlayer(java.util.UUID)
     * @deprecated Persistent storage of users should be by UUID as names are no longer
     *             unique past a single session.
     */
    @Deprecated
    @NotNull
    public SavablePlayer getOfflinePlayer(@NotNull String name);

    /**
     * Gets the player by the given UUID, regardless if they are offline or
     * online.
     * <p>
     * This will return an object even if the player does not exist. To this
     * method, all players will exist.
     *
     * @param id the UUID of the player to retrieve
     * @return an offline player
     */
    @NotNull
    public SavablePlayer getOfflinePlayer(@NotNull UUID id);

    /**
     * Creates a new {@link SavablePlayer}.
     *
     * @param uniqueId the unique id
     * @param name the name
     * @return the new PlayerProfile
     * @throws IllegalArgumentException if both the unique id is
     * <code>null</code> and the name is <code>null</code> or blank
     */
    @NotNull
    SavablePlayer createPlayerProfile(@Nullable UUID uniqueId, @Nullable String name);

    /**
     * Creates a new {@link SavablePlayer}.
     *
     * @param uniqueId the unique id
     * @return the new PlayerProfile
     * @throws IllegalArgumentException if the unique id is <code>null</code>
     */
    @NotNull
    SavablePlayer createPlayerProfile(@NotNull UUID uniqueId);

    /**
     * Creates a new {@link SavablePlayer}.
     *
     * @param name the name
     * @return the new PlayerProfile
     * @throws IllegalArgumentException if the name is <code>null</code> or
     * blank
     */
    @NotNull
    SavablePlayer createPlayerProfile(@NotNull String name);

    /**
     * Gets a set containing all current IPs that are banned.
     *
     * @return a set containing banned IP addresses
     */
    @NotNull
    public Set<String> getIPBans();

    /**
     * Bans the specified address from the plugin.
     *
     * @param address the IP address to ban
     */
    public void banIP(@NotNull String address);

    /**
     * Unbans the specified address from the plugin.
     *
     * @param address the IP address to unban
     */
    public void unbanIP(@NotNull String address);

    /**
     * Gets a set containing all banned players.
     *
     * @return a set containing banned players
     */
    @NotNull
    public Set<SavablePlayer> getBannedPlayers();

    /**
     * Gets a ban list for the supplied type.
     * <p>
     * Bans by name are no longer supported and this method will return
     * null when trying to request them. The replacement is bans by UUID.
     *
     * @param type the type of list to fetch, cannot be null
     * @return a ban list of the specified type
     */
    @NotNull
    public BanList getBanList(@NotNull BanList.Type type);

    /**
     * Gets a set containing all player operators.
     *
     * @return a set containing player operators
     */
    @NotNull
    public Set<SavablePlayer> getOperators();

    /**
     * Gets a {@link ConsoleCommandSender} that may be used as an input source
     * for this plugin.
     *
     * @return a console command sender
     */
    @NotNull
    public ConsoleCommandSender getConsoleSender();

    /**
     * Gets every player that has ever played on this plugin.
     *
     * @return an array containing all previous players
     */
    @NotNull
    public SavablePlayer[] getOfflinePlayers();

    /**
     * Gets the {@link HelpMap} providing help topics for this plugin.
     *
     * @return a help map for this plugin
     */
    @NotNull
    public HelpMap getHelpMap();

    /**
     * Checks the current thread against the expected primary thread for the
     * plugin.
     * <p>
     * <b>Note:</b> this method should not be used to indicate the current
     * synchronized state of the runtime. A current thread matching the main
     * thread indicates that it is synchronized, but a mismatch <b>does not
     * preclude</b> the same assumption.
     *
     * @return true if the current thread matches the expected primary thread,
     *     false otherwise
     */
    boolean isPrimaryThread();

    /**
     * Gets the message that is displayed on the plugin list.
     *
     * @return the plugins MOTD
     */
    @NotNull
    String getMotd();

    /**
     * Gets the default message that is displayed when the plugin is stopped.
     *
     * @return the shutdown message
     */
    @Nullable
    String getShutdownMessage();

    /**
     * Gets the current warning state for the plugin.
     *
     * @return the configured warning state
     */

    @NotNull
    public Warning.WarningState getWarningState();
    /**
     * Gets a tag which has already been defined within the plugin. Plugins are
     * suggested to use the concrete tags in {@link Tag} rather than this method
     * which makes no guarantees about which tags are available, and may also be
     * less performant due to lack of caching.
     * <br>
     * Tags will be searched for in an implementation specific manner, but a
     * path consisting of namespace/tags/registry/key is expected.
     * <br>
     * Plugin implementations are allowed to handle only the registries
     * indicated in {@link Tag}.
     *
     * @param <T> type of the tag
     * @param registry the tag registry to look at
     * @param tag the name of the tag
     * @param clazz the class of the tag entries
     * @return the tag or null
     */
    @Nullable
    <T extends Keyed> Tag<T> getTag(@NotNull String registry, @NotNull NamespacedKey tag, @NotNull Class<T> clazz);

    /**
     * Gets a all tags which have been defined within the plugin.
     * <br>
     * Plugin implementations are allowed to handle only the registries
     * indicated in {@link Tag}.
     * <br>
     * No guarantees are made about the mutability of the returned iterator.
     *
     * @param <T> type of the tag
     * @param registry the tag registry to look at
     * @param clazz the class of the tag entries
     * @return all defined tags
     */
    @NotNull
    <T extends Keyed> Iterable<Tag<T>> getTags(@NotNull String registry, @NotNull Class<T> clazz);

    /**
     * @return the unsafe values instance
     * @see UnsafeValues
     */
    @Deprecated
    @NotNull
    UnsafeValues getUnsafe();
}
