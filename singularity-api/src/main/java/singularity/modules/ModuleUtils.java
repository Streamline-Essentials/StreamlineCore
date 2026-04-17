package singularity.modules;

import gg.drak.thebase.events.BaseEventListener;
import singularity.Singularity;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.server.CosmicServer;
import singularity.interfaces.ISingularityExtension;
import singularity.interfaces.audiences.real.RealSender;
import singularity.interfaces.audiences.real.RealPlayer;
import singularity.objects.CosmicResourcePack;
import singularity.placeholders.RATRegistry;
import singularity.events.CosmicEvent;
import singularity.objects.CosmicTitle;
import singularity.scheduler.ModuleTaskManager;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Static utility facade for module authors that aggregates the most commonly
 * needed operations from {@link MessageUtils}, {@link UserUtils},
 * {@link ModuleManager}, and the core {@link Singularity} instance into a
 * single, convenient access point.
 *
 * <p>All methods are static; this class is not intended to be instantiated.
 */
public class ModuleUtils {

    /**
     * Returns the console-log prefix string for the given module, including
     * colour codes.
     *
     * @param module the module whose prefix is requested
     * @return the formatted prefix string
     */
    public static String loggedModulePrefix(CosmicModule module) {
        return MessageUtils.loggedModulePrefix(module);
    }

    /**
     * Logs a message at the INFO level on behalf of a module.
     *
     * @param module  the originating module
     * @param message the message to log
     */
    public static void logInfo(ModuleLike module, String message) {
        MessageUtils.logInfo(module, message);
    }

    /**
     * Logs a message at the WARNING level on behalf of a module.
     *
     * @param module  the originating module
     * @param message the message to log
     */
    public static void logWarning(ModuleLike module, String message) {
        MessageUtils.logWarning(module, message);
    }

    /**
     * Logs a message at the SEVERE level on behalf of a module.
     *
     * @param module  the originating module
     * @param message the message to log
     */
    public static void logSevere(ModuleLike module, String message) {
        MessageUtils.logSevere(module, message);
    }

    /**
     * Logs a message at the DEBUG level on behalf of a module.
     *
     * @param module  the originating module
     * @param message the message to log
     */
    public static void logDebug(ModuleLike module, String message) {
        MessageUtils.logDebug(module, message);
    }

    /**
     * Logs a stack trace at the INFO level on behalf of a module.
     *
     * @param module   the originating module
     * @param elements the stack trace to log
     */
    public static void logInfo(ModuleLike module, StackTraceElement[] elements) {
        MessageUtils.logInfo(module, elements);
    }

    /**
     * Logs a stack trace at the WARNING level on behalf of a module.
     *
     * @param module   the originating module
     * @param elements the stack trace to log
     */
    public static void logWarning(ModuleLike module, StackTraceElement[] elements) {
        MessageUtils.logWarning(module, elements);
    }

    /**
     * Logs a stack trace at the SEVERE level on behalf of a module.
     *
     * @param module   the originating module
     * @param elements the stack trace to log
     */
    public static void logSevere(ModuleLike module, StackTraceElement[] elements) {
        MessageUtils.logSevere(module, elements);
    }

    /**
     * Logs a stack trace at the DEBUG level on behalf of a module.
     *
     * @param module   the originating module
     * @param elements the stack trace to log
     */
    public static void logDebug(ModuleLike module, StackTraceElement[] elements) {
        MessageUtils.logDebug(module, elements);
    }

    /**
     * Sends a plain (or colour-coded) message to the given sender.
     *
     * @param to      the target sender, or {@code null} to send to nobody
     * @param message the message text (supports colour codes)
     */
    public static void sendMessage(@Nullable CosmicSender to, String message) {
        Singularity.getInstance().getMessenger().sendMessage(to, message);
    }

    /**
     * Sends a message to {@code to}, replacing placeholders relative to
     * another sender identified by UUID.
     *
     * @param to        the target sender, or {@code null}
     * @param otherUUID the UUID of the sender whose context is used for
     *                  placeholder substitution
     * @param message   the message text
     */
    public static void sendMessage(@Nullable CosmicSender to, String otherUUID, String message) {
        Singularity.getInstance().getMessenger().sendMessage(to, otherUUID, message);
    }

    /**
     * Sends a message to {@code to}, replacing placeholders relative to the
     * given {@code other} sender.
     *
     * @param to      the target sender, or {@code null}
     * @param other   the sender whose context is used for placeholder
     *                substitution
     * @param message the message text
     */
    public static void sendMessage(@Nullable CosmicSender to, CosmicSender other, String message) {
        Singularity.getInstance().getMessenger().sendMessage(to, other, message);
    }

    /**
     * Sends a message to the sender identified by UUID string.
     *
     * @param to      the UUID string of the target sender
     * @param message the message text
     */
    public static void sendMessage(String to, String message) {
        MessageUtils.sendMessage(to, message);
    }

    /**
     * Sends a message to the sender identified by {@code to}, using the
     * context of the sender identified by {@code otherUUID} for placeholder
     * substitution.
     *
     * @param to        the UUID string of the target sender, or {@code null}
     * @param otherUUID the UUID string of the context sender
     * @param message   the message text
     */
    public static void sendMessage(@Nullable String to, String otherUUID, String message) {
        MessageUtils.sendMessage(to, otherUUID, message);
    }

    /**
     * Sends a title (main + sub) to the given sender.
     *
     * @param user  the recipient
     * @param title the title to display
     */
    public static void sendTitle(CosmicSender user, CosmicTitle title) {
        Singularity.getInstance().getMessenger().sendTitle(user, title);
    }

    /**
     * Returns a human-readable, comma-separated representation of the given
     * list.
     *
     * @param list the list to format
     * @return the formatted string
     */
    public static String getListAsFormattedString(List<?> list) {
        return MessageUtils.getListAsFormattedString(list);
    }

    /**
     * Removes a trailing period from the end of the string if present.
     *
     * @param string the input string
     * @return the string without a trailing dot
     */
    public static String removeExtraDot(String string){
        return MessageUtils.removeExtraDot(string);
    }

    /**
     * Left-pads or right-pads the given text to exactly {@code digits}
     * characters.
     *
     * @param text   the input text
     * @param digits the desired width
     * @return the resized string
     */
    public static String resize(String text, int digits) {
        return MessageUtils.resize(text, digits);
    }

    /**
     * Truncates the given text to at most {@code digits} characters.
     *
     * @param text   the input text
     * @param digits the maximum number of characters to keep
     * @return the truncated string
     */
    public static String truncate(String text, int digits) {
        return MessageUtils.truncate(text, digits);
    }

    /**
     * Calculates the number of digits needed when the start position is added
     * to the other size.
     *
     * @param start     the starting offset
     * @param otherSize the other measurement to add
     * @return the combined digit count
     */
    public static int getDigits(int start, int otherSize){
        return MessageUtils.getDigits(start, otherSize);
    }

    /**
     * Filters a list to only those entries that start with the given parameter
     * string (case-insensitive tab-completion matching).
     *
     * @param of    the full list of candidates
     * @param param the current input prefix
     * @return a sorted set of matching candidates
     */
    public static ConcurrentSkipListSet<String> getCompletion(List<String> of, String param){
        return MessageUtils.getCompletion(of, param);
    }

    /**
     * Filters a sorted set to only those entries that start with the given
     * parameter string (case-insensitive tab-completion matching).
     *
     * @param of    the full set of candidates
     * @param param the current input prefix
     * @return a sorted set of matching candidates
     */
    public static ConcurrentSkipListSet<String> getCompletion(ConcurrentSkipListSet<String> of, String param){
        return MessageUtils.getCompletion(of, param);
    }

    /**
     * Strips all colour-code formatting from the given string.
     *
     * @param string the colour-coded string
     * @return the plain-text equivalent
     */
    public static String stripColor(String string){
        return Singularity.getInstance().getMessenger().stripColor(string);
    }

    /**
     * Returns a copy of the args array with the elements at the given indices
     * removed.
     *
     * @param args     the original argument array
     * @param toRemove zero-based indices to remove
     * @return a new array without the specified elements
     */
    public static String[] argsMinus(String[] args, int... toRemove) {
        return MessageUtils.argsMinus(args, toRemove);
    }

    /**
     * Joins all arguments (minus the excluded indices) into a single
     * space-separated string.
     *
     * @param args     the original argument array
     * @param toRemove zero-based indices to exclude
     * @return the joined string
     */
    public static String argsToStringMinus(String[] args, int... toRemove){
        return MessageUtils.argsToStringMinus(args, toRemove);
    }

    /**
     * Joins all arguments into a single space-separated string.
     *
     * @param args the argument array
     * @return the joined string
     */
    public static String argsToString(String[] args){
        return MessageUtils.argsToString(args);
    }

    /**
     * Translates ampersand-prefixed colour codes in the given text to their
     * section-sign equivalents.
     *
     * @param text the input text with {@code &}-codes
     * @return the colour-formatted string
     */
    public static String codedString(String text){
        return MessageUtils.codedString(text);
    }

    /**
     * Applies full formatting (colour codes and placeholder newlines) to the
     * given string.
     *
     * @param string the raw string to format
     * @return the fully formatted string
     */
    public static String formatted(String string) {
        return MessageUtils.formatted(string);
    }

    /**
     * Replaces {@code %newline%} tokens in the given text with actual newline
     * characters.
     *
     * @param text the input text
     * @return the text with literal newlines
     */
    public static String newLined(String text){
        return MessageUtils.newLined(text);
    }

    /**
     * Returns {@code true} if the given message string looks like a command
     * (i.e. starts with {@code /}).
     *
     * @param msg the message to test
     * @return {@code true} if it appears to be a command
     */
    public static boolean isCommand(String msg){
        return MessageUtils.isCommand(msg);
    }

    /**
     * Joins an array of strings into a single space-separated string.
     *
     * @param splitMsg the array to normalise
     * @return the joined string
     */
    public static String normalize(String[] splitMsg){
        return MessageUtils.normalize(splitMsg);
    }

    /**
     * Joins a {@link TreeSet} of strings into a single space-separated string.
     *
     * @param splitMsg the set to normalise
     * @return the joined string
     */
    public static String normalize(TreeSet<String> splitMsg) {
        return MessageUtils.normalize(splitMsg);
    }

    /**
     * Joins the values of a {@link TreeMap} (ordered by key) into a single
     * space-separated string.
     *
     * @param splitMsg the map whose values are joined
     * @return the joined string
     */
    public static String normalize(TreeMap<Integer, String> splitMsg) {
        return MessageUtils.normalize(splitMsg);
    }

    /**
     * Returns {@code true} if {@code object} equals every element in
     * {@code toEqual}.
     *
     * @param object  the reference object
     * @param toEqual the objects to compare against
     * @return {@code true} when all comparisons are equal
     */
    public static boolean equalsAll(Object object, Object... toEqual){
        return MessageUtils.equalsAll(object, toEqual);
    }

    /**
     * Returns {@code true} if {@code object} equals every element in the
     * given collection.
     *
     * @param object  the reference object
     * @param toEqual the collection of objects to compare against
     * @return {@code true} when all comparisons are equal
     */
    public static boolean equalsAll(Object object, Collection<Object> toEqual){
        return MessageUtils.equalsAll(object, toEqual);
    }

    /**
     * Returns {@code true} if {@code object} equals at least one element in
     * the given collection.
     *
     * @param object  the reference object
     * @param toEqual the collection of candidates
     * @return {@code true} when at least one comparison is equal
     */
    public static boolean equalsAny(Object object, Collection<?> toEqual){
        return MessageUtils.equalsAny(object, toEqual);
    }

    /**
     * Replaces all Streamline placeholders in the string relative to the
     * given sender, including BungeeCord-style user tags.
     *
     * @param user the context sender for placeholder resolution
     * @param of   the string containing placeholders
     * @return the string with all placeholders replaced
     */
    public static String replaceAllPlayerBungee(CosmicSender user, String of) {
        return MessageUtils.replaceAllPlayerBungee(user, of);
    }

    /**
     * Replaces all Streamline placeholders in the string for the sender
     * identified by UUID string.
     *
     * @param uuid the UUID string of the context sender
     * @param of   the string containing placeholders
     * @return the string with all placeholders replaced
     */
    public static String replaceAllPlayerBungee(String uuid, String of) {
        return MessageUtils.replaceAllPlayerBungee(uuid, of);
    }

    /**
     * Converts a comma-delimited string into a {@link List}.
     *
     * @param string the comma-separated input
     * @return the list of trimmed tokens
     */
    public static List<String> getStringListFromString(String string) {
        return MessageUtils.getStringListFromString(string);
    }

    /**
     * Returns {@code true} if the array is {@code null} or its length is less
     * than or equal to {@code lessThanOrEqualTo}.
     *
     * @param thingArray        the array to check
     * @param lessThanOrEqualTo the threshold length
     * @return {@code true} when the condition holds
     */
    public static boolean isNullOrLessThanEqualTo(Object[] thingArray, int lessThanOrEqualTo) {
        return MessageUtils.isNullOrLessThanEqualTo(thingArray, lessThanOrEqualTo);
    }

    /**
     * Returns a snapshot of all currently loaded (cached) senders keyed by
     * UUID string.
     *
     * @return a sorted map of UUID to {@link CosmicSender}
     */
    public static ConcurrentSkipListMap<String, CosmicSender> getLoadedSenders() {
        return UserUtils.getLoadedSenders();
    }

    /**
     * Returns a snapshot of all currently loaded (cached) players keyed by
     * UUID string.
     *
     * @return a sorted map of UUID to {@link CosmicPlayer}
     */
    public static ConcurrentSkipListMap<String, CosmicPlayer> getLoadedPlayers() {
        return UserUtils.getLoadedPlayers();
    }

    /**
     * Returns a snapshot of all currently online senders keyed by UUID string.
     *
     * @return a sorted map of UUID to {@link CosmicSender}
     */
    public static ConcurrentSkipListMap<String, CosmicSender> getOnlineUsers() {
        return UserUtils.getOnlineSenders();
    }

    /**
     * Returns a snapshot of all currently online players keyed by UUID string.
     *
     * @return a sorted map of UUID to {@link CosmicPlayer}
     */
    public static ConcurrentSkipListMap<String, CosmicPlayer> getOnlinePlayers() {
        return UserUtils.getOnlinePlayers();
    }

    /**
     * Returns all loaded senders as a sorted set.
     *
     * @return a {@link ConcurrentSkipListSet} of all loaded senders
     */
    public static ConcurrentSkipListSet<CosmicSender> getLoadedSendersSet() {
        return UserUtils.getLoadedSendersSet();
    }

    /**
     * Returns all loaded players as a sorted set.
     *
     * @return a {@link ConcurrentSkipListSet} of all loaded players
     */
    public static ConcurrentSkipListSet<CosmicPlayer> getLoadedPlayersSet() {
        return UserUtils.getLoadedPlayersSet();
    }

    /**
     * Ensures the given sender is present in the loaded-sender cache,
     * inserting it if necessary.
     *
     * @param user the sender to load
     * @return the (potentially pre-existing) cached sender instance
     */
    public static CosmicSender loadSender(CosmicSender user) {
        return UserUtils.loadSender(user);
    }

    /**
     * Ensures the given player is present in the loaded-player cache,
     * inserting it if necessary.
     *
     * @param user the player to load
     * @return the (potentially pre-existing) cached player instance
     */
    public static CosmicPlayer loadPlayer(CosmicPlayer user) {
        return UserUtils.loadPlayer(user);
    }

    /**
     * Removes the given sender from the loaded-sender cache.
     *
     * @param user the sender to unload
     */
    public static void unloadUser(CosmicSender user) {
        UserUtils.unloadSender(user);
    }

    /**
     * Returns {@code true} if a persisted user record exists for the given
     * UUID string.
     *
     * @param uuid the UUID string to check
     * @return {@code true} when the user exists in storage
     */
    public static boolean userExists(String uuid) {
        return UserUtils.userExists(uuid);
    }

    /**
     * Returns an {@link Optional} containing the player for the given UUID,
     * creating one from storage if not already cached.
     *
     * @param uuid the UUID string of the player
     * @return an {@link Optional} with the player, or empty if not found
     */
    public static Optional<CosmicPlayer> getOrCreatePlayer(String uuid) {
        return UserUtils.getOrCreatePlayer(uuid);
    }

    /**
     * Returns an {@link Optional} containing the sender for the given UUID,
     * creating one from storage if not already cached.
     *
     * @param uuid the UUID string of the sender
     * @return an {@link Optional} with the sender, or empty if not found
     */
    public static Optional<CosmicSender> getOrCreateSender(String uuid) {
        return UserUtils.getOrCreateSender(uuid);
    }

    /**
     * Returns {@code true} if the given UUID string belongs to the server
     * console sender.
     *
     * @param uuid the UUID string to check
     * @return {@code true} if it represents the console
     */
    public static boolean isConsole(String uuid) {
        return UserUtils.isConsole(uuid);
    }

    /**
     * Returns {@code true} if the player identified by the given UUID is
     * currently online according to the platform's user manager.
     *
     * @param uuid the UUID string to check
     * @return {@code true} if the player is online
     */
    public static boolean isOnline(String uuid) {
        return Singularity.getInstance().getUserManager().isOnline(uuid);
    }

    /**
     * Returns a colour-formatted string indicating whether the given sender is
     * online or offline (e.g. {@code "&aOnline"} / {@code "&cOffline"}).
     *
     * @param stat the sender to check
     * @return the formatted online/offline string
     */
    public static String getOffOnFormatted(CosmicSender stat){
        return UserUtils.getOffOnFormatted(stat);
    }

    /**
     * Returns an unformatted string indicating whether the given sender is
     * online or offline (e.g. {@code "Online"} / {@code "Offline"}).
     *
     * @param stat the sender to check
     * @return the plain online/offline string
     */
    public static String getOffOnAbsolute(CosmicSender stat){
        return UserUtils.getOffOnAbsolute(stat);
    }

    /**
     * Returns a colour-formatted representation of the given sender (e.g.
     * includes prefix, name, and suffix).
     *
     * @param stat the sender to format
     * @return the formatted string
     */
    public static String getFormatted(CosmicSender stat){
        return UserUtils.getFormatted(stat);
    }

    /**
     * Returns the plain (unformatted) name of the given sender.
     *
     * @param stat the sender
     * @return the absolute (raw) name string
     */
    public static String getAbsolute(CosmicSender stat){
        return UserUtils.getAbsolute(stat);
    }

    /**
     * Returns the display name of the given sender as reported by the
     * sender object itself.
     *
     * @param user the sender
     * @return the display name string
     */
    public static String getDisplayName(CosmicSender user) {
        return user.getDisplayName();
    }

    /**
     * Fires a {@link CosmicEvent} through the module manager's event bus.
     *
     * @param event the event to fire; must not be {@code null}
     */
    public static void fireEvent(CosmicEvent event) {
        ModuleManager.fireEvent(event);
    }

    /**
     * Registers a {@link BaseEventListener} with the given module so that
     * its handler methods receive events.
     *
     * @param listener the listener to register
     * @param module   the owning module
     */
    public static void listen(BaseEventListener listener, CosmicModule module) {
        ModuleManager.registerEvents(listener, module);
    }

    /**
     * Returns the names of all players currently online across all servers as
     * seen by the platform.
     *
     * @return a sorted set of online player names
     */
    public static ConcurrentSkipListSet<String> getOnlinePlayerNames() {
        return Singularity.getInstance().getPlatform().getOnlinePlayerNames();
    }

    /**
     * Returns {@code true} if the given sender has the specified permission
     * node.
     *
     * @param user       the sender to check
     * @param permission the permission node
     * @return {@code true} if the sender has the permission
     * @deprecated Check permissions directly on the sender object instead.
     */
    @Deprecated
    public static boolean hasPermission(CosmicSender user, String permission) {
        return user.hasPermission(permission);
    }

    /**
     * Returns the platform's console sender wrapper.
     *
     * @return the console {@link RealSender}
     */
    public static RealSender<?> getConsole() {
        return Singularity.getConsole();
    }

    /**
     * Returns the platform player wrapper for the given UUID string, or
     * {@code null} if the player is not online.
     *
     * @param uuid the UUID string of the player
     * @return the {@link RealPlayer} wrapper, or {@code null}
     */
    public static RealPlayer<?> getPlayer(String uuid) {
        return Singularity.getPlayer(uuid);
    }

//    public static boolean runAs(OperatorUser user, String command) {
//        return UserUtils.runAs(user, command);
//    }

    /**
     * Executes the given command string as the specified sender.
     *
     * @param user    the sender who should execute the command
     * @param command the command string (without leading {@code /})
     * @return {@code true} if the command was executed successfully
     */
    public static boolean runAs(CosmicSender user, String command) {
        return UserUtils.runAs(user, command);
    }

    /**
     * Queues a command to be executed as the given sender on the next
     * available tick.
     *
     * @param user    the sender who will execute the command
     * @param command the command string to queue
     */
    public static void queueRunAs(CosmicSender user, String command) {
        Singularity.addCachedCommand(command, user);
    }

    /**
     * Executes a command as the given player, optionally bypassing permission
     * checks.
     *
     * @param user    the player who should execute the command
     * @param bypass  {@code true} to execute with elevated (bypassed)
     *                permissions
     * @param command the command string to execute
     * @return {@code true} if the command was executed successfully
     */
    public static boolean runAs(CosmicPlayer user, boolean bypass, String command) {
        return Singularity.getInstance().getUserManager().runAs(user, bypass, command);
    }

    /**
     * Looks up the UUID string for a player by their display name.
     *
     * @param name the player name to resolve
     * @return an {@link Optional} containing the UUID string, or empty if not
     *         found
     */
    public static Optional<String> getUUIDFromName(String name) {
        return UserUtils.getUUIDFromName(name);
    }

    /**
     * Returns an {@link Optional} containing the sender with the given name,
     * loading them from storage if necessary.
     *
     * @param name the player/sender name to look up
     * @return an {@link Optional} with the sender, or empty if not found
     */
    public static Optional<CosmicSender> getOrGetUserByName(String name) {
        return UserUtils.getOrCreateSenderByName(name);
    }

    /**
     * Makes the given sender send a chat message as themselves.
     *
     * @param as      the sender who sends the message
     * @param message the chat text
     * @deprecated Use {@link CosmicSender#chatAs(String)} directly.
     */
    @Deprecated
    public static void chatAs(CosmicSender as, String message) {
        as.chatAs(message);
    }

    /**
     * Forces the given sender to run a command string.
     *
     * @param as      the sender who runs the command
     * @param message the command string
     * @deprecated Use {@link CosmicSender#runCommand(String)} directly.
     */
    @Deprecated
    public static void runAsStrictly(CosmicSender as, String message) {
        as.runCommand(message);
    }

    /**
     * Returns all players currently on the specified server.
     *
     * @param server the server name to query
     * @return a sorted set of players on that server
     */
    public static ConcurrentSkipListSet<CosmicPlayer> getUsersOn(String server) {
        return Singularity.getInstance().getUserManager().getUsersOn(server);
    }

    /**
     * Returns the names of all servers known to the platform.
     *
     * @return a sorted set of server name strings
     */
    public static ConcurrentSkipListSet<String> getServerNames() {
        return Singularity.getInstance().getPlatform().getServerNames();
    }

    /**
     * Connects the given sender to the named server.  If the sender is not a
     * {@link CosmicPlayer} the request is silently ignored.
     *
     * @param user   the sender to connect
     * @param server the target server name
     */
    public static void connect(CosmicSender user, String server) {
        if (! (user instanceof CosmicPlayer)) {
//            user.setServerName(server);
            return;
        }
        CosmicPlayer player = (CosmicPlayer) user;

        Singularity.getInstance().getUserManager().connect(player, server);
    }

    /**
     * Connects the given sender to the server represented by the given
     * {@link CosmicServer}.
     *
     * @param user   the sender to connect
     * @param server the target server
     */
    public static void connect(CosmicSender user, CosmicServer server) {
        connect(user, server.getIdentifier());
    }

    /**
     * Returns {@code true} if the given player is connected through the
     * Geyser (Bedrock-Java bridging) proxy.
     *
     * @param user the player to check
     * @return {@code true} if the player is a Geyser player
     */
    public static boolean isGeyserPlayer(CosmicPlayer user) {
        return UserUtils.isGeyserPlayer(user);
    }

    /**
     * Returns {@code true} if the player with the given UUID is connected
     * through the Geyser proxy.
     *
     * @param uuid the UUID string of the player
     * @return {@code true} if the player is a Geyser player
     */
    public static boolean isGeyserPlayer(String uuid) {
        return UserUtils.isGeyserPlayer(uuid);
    }

    /**
     * Returns {@code true} if the current server platform has a plugin with
     * the given name loaded.
     *
     * @param plugin the plugin name to check
     * @return {@code true} if the plugin is present
     */
    public static boolean serverHasPlugin(String plugin) {
        return Singularity.getInstance().getPlatform().serverHasPlugin(plugin);
    }

    /**
     * Returns {@code true} if the given server name matches any server known
     * to the platform.
     *
     * @param servername the server name to test
     * @return {@code true} if it matches a known server
     */
    public static boolean equalsAnyServer(String servername) {
        return Singularity.getInstance().getPlatform().equalsAnyServer(servername);
    }

    /**
     * Returns the shared {@link ModuleTaskManager} used to schedule tasks on
     * behalf of modules.
     *
     * @return the module task scheduler
     */
    public static ModuleTaskManager getModuleScheduler() {
        return Singularity.getModuleScheduler();
    }

    /**
     * Returns the platform type the server is currently running on (e.g.
     * VELOCITY, BUNGEE, SPIGOT).
     *
     * @return the current {@link ISingularityExtension.PlatformType}
     */
    public static ISingularityExtension.PlatformType getPlatformType() {
        return Singularity.getInstance().getPlatform().getPlatformType();
    }

    /**
     * Returns whether the current server is a proxy or a backend (game)
     * server.
     *
     * @return the current {@link ISingularityExtension.ServerType}
     */
    public static ISingularityExtension.ServerType getServerType() {
        return Singularity.getInstance().getPlatform().getServerType();
    }

    /**
     * Sends a resource pack to the specified player via the platform.
     *
     * @param resourcePack the resource pack to send
     * @param player       the target player
     */
    public static void sendResourcePack(CosmicResourcePack resourcePack, CosmicPlayer player) {
        Singularity.getInstance().getPlatform().sendResourcePack(resourcePack, player);
    }

    /**
     * Returns the last-measured network ping of the player with the given UUID
     * in milliseconds.
     *
     * @param uuid the UUID string of the player
     * @return the ping in milliseconds
     */
    public static double getPlayerPing(String uuid) {
        return Singularity.getInstance().getUserManager().getPlayerPing(uuid);
    }

    /**
     * Returns the main class loader used by the platform plugin, which can be
     * used to load resources or classes from the main JAR.
     *
     * @return the platform's main {@link ClassLoader}
     */
    public static ClassLoader getMainClassLoader() {
        return Singularity.getInstance().getPlatform().getMainClassLoader();
    }

    /**
     * Parses a string on the proxy side using the given sender as the
     * placeholder context.
     *
     * @param user    the context sender
     * @param toParse the string containing placeholders to resolve
     * @return the parsed string
     */
    public static String parseOnProxy(CosmicSender user, String toParse) {
        return MessageUtils.parseOnProxy(user, toParse);
    }

    /**
     * Parses a string on the proxy side using the console as the placeholder
     * context.
     *
     * @param toParse the string containing placeholders to resolve
     * @return the parsed string
     */
    public static String parseOnProxy(String toParse) {
        return MessageUtils.parseOnProxy(UserUtils.getConsole(), toParse);
    }

    /**
     * Kicks the specified player from the server with the given reason message.
     *
     * @param user    the player to kick
     * @param message the kick reason (supports colour codes)
     */
    public static void kick(CosmicPlayer user, String message) {
        Singularity.getInstance().getUserManager().kick(user, message);
    }

    /**
     * Kicks the specified player from the server with a default
     * {@code "&cConnection Closed by Server"} message.
     *
     * @param user the player to kick
     */
    public static void kick(CosmicPlayer user) {
        Singularity.getInstance().getUserManager().kick(user, "&cConnection Closed by Server");
    }

    /**
     * Teleports a player to the location of the target player.
     *
     * @param player the player to teleport
     * @param target the target player whose location is used
     * @deprecated Use {@link UserUtils#teleport(CosmicSender, CosmicPlayer)} instead.
     */
    @Deprecated(since = "2.5.5.0")
    public static void teleport(CosmicPlayer player, CosmicPlayer target) {
        UserUtils.teleport(player, target);
    }

    /**
     * Teleports the given sender to the location of the target player.
     *
     * @param player the sender to teleport
     * @param target the target player whose location is used
     */
    public static void teleport(CosmicSender player, CosmicPlayer target) {
        UserUtils.teleport(player, target);
    }

    /**
     * Teleports the given sender to the specified location.
     *
     * @param player   the sender to teleport
     * @param location the destination location
     */
    public static void teleport(CosmicSender player, CosmicLocation location) {
        UserUtils.teleport(player, location);
    }

    /**
     * Replaces all registered RAT (Runtime Argument Translation) placeholders
     * in the given string using no specific user context.
     *
     * @param string the string containing placeholders
     * @return the string with all placeholders replaced
     */
    public static String replacePlaceholders(String string) {
        return RATRegistry.fetchDirty(string);
    }

    /**
     * Replaces all registered RAT placeholders in the given string using the
     * given sender as the context.
     *
     * @param user   the context sender for placeholder resolution
     * @param string the string containing placeholders
     * @return the string with all placeholders replaced
     */
    public static String replacePlaceholders(CosmicSender user, String string) {
        return RATRegistry.fetchDirty(string, user);
    }
}
