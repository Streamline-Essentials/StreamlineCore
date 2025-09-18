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

public class ModuleUtils {
    public static String loggedModulePrefix(CosmicModule module) {
        return MessageUtils.loggedModulePrefix(module);
    }

    public static void logInfo(ModuleLike module, String message) {
        MessageUtils.logInfo(module, message);
    }

    public static void logWarning(ModuleLike module, String message) {
        MessageUtils.logWarning(module, message);
    }

    public static void logSevere(ModuleLike module, String message) {
        MessageUtils.logSevere(module, message);
    }
    public static void logDebug(ModuleLike module, String message) {
        MessageUtils.logDebug(module, message);
    }
    public static void logInfo(ModuleLike module, StackTraceElement[] elements) {
        MessageUtils.logInfo(module, elements);
    }

    public static void logWarning(ModuleLike module, StackTraceElement[] elements) {
        MessageUtils.logWarning(module, elements);
    }

    public static void logSevere(ModuleLike module, StackTraceElement[] elements) {
        MessageUtils.logSevere(module, elements);
    }
    public static void logDebug(ModuleLike module, StackTraceElement[] elements) {
        MessageUtils.logDebug(module, elements);
    }

    public static void sendMessage(@Nullable CosmicSender to, String message) {
        Singularity.getInstance().getMessenger().sendMessage(to, message);
    }

    public static void sendMessage(@Nullable CosmicSender to, String otherUUID, String message) {
        Singularity.getInstance().getMessenger().sendMessage(to, otherUUID, message);
    }

    public static void sendMessage(@Nullable CosmicSender to, CosmicSender other, String message) {
        Singularity.getInstance().getMessenger().sendMessage(to, other, message);
    }

    public static void sendMessage(String to, String message) {
        MessageUtils.sendMessage(to, message);
    }

    public static void sendMessage(@Nullable String to, String otherUUID, String message) {
        MessageUtils.sendMessage(to, otherUUID, message);
    }

    public static void sendTitle(CosmicSender user, CosmicTitle title) {
        Singularity.getInstance().getMessenger().sendTitle(user, title);
    }

    public static String getListAsFormattedString(List<?> list) {
        return MessageUtils.getListAsFormattedString(list);
    }

    public static String removeExtraDot(String string){
        return MessageUtils.removeExtraDot(string);
    }

    public static String resize(String text, int digits) {
        return MessageUtils.resize(text, digits);
    }

    public static String truncate(String text, int digits) {
        return MessageUtils.truncate(text, digits);
    }

    public static int getDigits(int start, int otherSize){
        return MessageUtils.getDigits(start, otherSize);
    }

    public static ConcurrentSkipListSet<String> getCompletion(List<String> of, String param){
        return MessageUtils.getCompletion(of, param);
    }

    public static ConcurrentSkipListSet<String> getCompletion(ConcurrentSkipListSet<String> of, String param){
        return MessageUtils.getCompletion(of, param);
    }

    public static String stripColor(String string){
        return Singularity.getInstance().getMessenger().stripColor(string);
    }

    public static String[] argsMinus(String[] args, int... toRemove) {
        return MessageUtils.argsMinus(args, toRemove);
    }

    public static String argsToStringMinus(String[] args, int... toRemove){
        return MessageUtils.argsToStringMinus(args, toRemove);
    }

    public static String argsToString(String[] args){
        return MessageUtils.argsToString(args);
    }

    public static String codedString(String text){
        return MessageUtils.codedString(text);
    }

    public static String formatted(String string) {
        return MessageUtils.formatted(string);
    }

    public static String newLined(String text){
        return MessageUtils.newLined(text);
    }

    public static boolean isCommand(String msg){
        return MessageUtils.isCommand(msg);
    }

    public static String normalize(String[] splitMsg){
        return MessageUtils.normalize(splitMsg);
    }

    public static String normalize(TreeSet<String> splitMsg) {
        return MessageUtils.normalize(splitMsg);
    }

    public static String normalize(TreeMap<Integer, String> splitMsg) {
        return MessageUtils.normalize(splitMsg);
    }

    public static boolean equalsAll(Object object, Object... toEqual){
        return MessageUtils.equalsAll(object, toEqual);
    }

    public static boolean equalsAll(Object object, Collection<Object> toEqual){
        return MessageUtils.equalsAll(object, toEqual);
    }

    public static boolean equalsAny(Object object, Collection<?> toEqual){
        return MessageUtils.equalsAny(object, toEqual);
    }

    public static String replaceAllPlayerBungee(CosmicSender user, String of) {
        return MessageUtils.replaceAllPlayerBungee(user, of);
    }

    public static String replaceAllPlayerBungee(String uuid, String of) {
        return MessageUtils.replaceAllPlayerBungee(uuid, of);
    }

    public static List<String> getStringListFromString(String string) {
        return MessageUtils.getStringListFromString(string);
    }

    public static boolean isNullOrLessThanEqualTo(Object[] thingArray, int lessThanOrEqualTo) {
        return MessageUtils.isNullOrLessThanEqualTo(thingArray, lessThanOrEqualTo);
    }

    public static ConcurrentSkipListMap<String, CosmicSender> getLoadedSenders() {
        return UserUtils.getLoadedSenders();
    }

    public static ConcurrentSkipListMap<String, CosmicPlayer> getLoadedPlayers() {
        return UserUtils.getLoadedPlayers();
    }

    public static ConcurrentSkipListMap<String, CosmicSender> getOnlineUsers() {
        return UserUtils.getOnlineSenders();
    }

    public static ConcurrentSkipListMap<String, CosmicPlayer> getOnlinePlayers() {
        return UserUtils.getOnlinePlayers();
    }

    public static ConcurrentSkipListSet<CosmicSender> getLoadedSendersSet() {
        return UserUtils.getLoadedSendersSet();
    }

    public static ConcurrentSkipListSet<CosmicPlayer> getLoadedPlayersSet() {
        return UserUtils.getLoadedPlayersSet();
    }

    public static CosmicSender loadSender(CosmicSender user) {
        return UserUtils.loadSender(user);
    }

    public static CosmicPlayer loadPlayer(CosmicPlayer user) {
        return UserUtils.loadPlayer(user);
    }

    public static void unloadUser(CosmicSender user) {
        UserUtils.unloadSender(user);
    }

    public static boolean userExists(String uuid) {
        return UserUtils.userExists(uuid);
    }

    public static Optional<CosmicPlayer> getOrCreatePlayer(String uuid) {
        return UserUtils.getOrCreatePlayer(uuid);
    }

    public static Optional<CosmicSender> getOrCreateSender(String uuid) {
        return UserUtils.getOrCreateSender(uuid);
    }

    public static boolean isConsole(String uuid) {
        return UserUtils.isConsole(uuid);
    }

    public static boolean isOnline(String uuid) {
        return Singularity.getInstance().getUserManager().isOnline(uuid);
    }

    public static String getOffOnFormatted(CosmicSender stat){
        return UserUtils.getOffOnFormatted(stat);
    }

    public static String getOffOnAbsolute(CosmicSender stat){
        return UserUtils.getOffOnAbsolute(stat);
    }

    public static String getFormatted(CosmicSender stat){
        return UserUtils.getFormatted(stat);
    }

    public static String getAbsolute(CosmicSender stat){
        return UserUtils.getAbsolute(stat);
    }

    public static String getDisplayName(CosmicSender user) {
        return user.getDisplayName();
    }

    public static void fireEvent(CosmicEvent event) {
        ModuleManager.fireEvent(event);
    }

    public static void listen(BaseEventListener listener, CosmicModule module) {
        ModuleManager.registerEvents(listener, module);
    }

    public static ConcurrentSkipListSet<String> getOnlinePlayerNames() {
        return Singularity.getInstance().getPlatform().getOnlinePlayerNames();
    }

    @Deprecated
    public static boolean hasPermission(CosmicSender user, String permission) {
        return user.hasPermission(permission);
    }

    public static RealSender<?> getConsole() {
        return Singularity.getConsole();
    }

    public static RealPlayer<?> getPlayer(String uuid) {
        return Singularity.getPlayer(uuid);
    }

//    public static boolean runAs(OperatorUser user, String command) {
//        return UserUtils.runAs(user, command);
//    }

    public static boolean runAs(CosmicSender user, String command) {
        return UserUtils.runAs(user, command);
    }

    public static void queueRunAs(CosmicSender user, String command) {
        Singularity.addCachedCommand(command, user);
    }

    public static boolean runAs(CosmicPlayer user, boolean bypass, String command) {
        return Singularity.getInstance().getUserManager().runAs(user, bypass, command);
    }

    public static Optional<String> getUUIDFromName(String name) {
        return UserUtils.getUUIDFromName(name);
    }

    public static Optional<CosmicSender> getOrGetUserByName(String name) {
        return UserUtils.getOrCreateSenderByName(name);
    }

    @Deprecated
    public static void chatAs(CosmicSender as, String message) {
        as.chatAs(message);
    }

    @Deprecated
    public static void runAsStrictly(CosmicSender as, String message) {
        as.runCommand(message);
    }

    public static ConcurrentSkipListSet<CosmicPlayer> getUsersOn(String server) {
        return Singularity.getInstance().getUserManager().getUsersOn(server);
    }

    public static ConcurrentSkipListSet<String> getServerNames() {
        return Singularity.getInstance().getPlatform().getServerNames();
    }

    public static void connect(CosmicSender user, String server) {
        if (! (user instanceof CosmicPlayer)) {
//            user.setServerName(server);
            return;
        }
        CosmicPlayer player = (CosmicPlayer) user;

        Singularity.getInstance().getUserManager().connect(player, server);
    }

    public static void connect(CosmicSender user, CosmicServer server) {
        connect(user, server.getIdentifier());
    }

    public static boolean isGeyserPlayer(CosmicPlayer user) {
        return UserUtils.isGeyserPlayer(user);
    }

    public static boolean isGeyserPlayer(String uuid) {
        return UserUtils.isGeyserPlayer(uuid);
    }

    public static boolean serverHasPlugin(String plugin) {
        return Singularity.getInstance().getPlatform().serverHasPlugin(plugin);
    }

    public static boolean equalsAnyServer(String servername) {
        return Singularity.getInstance().getPlatform().equalsAnyServer(servername);
    }

    public static ModuleTaskManager getModuleScheduler() {
        return Singularity.getModuleScheduler();
    }

    public static ISingularityExtension.PlatformType getPlatformType() {
        return Singularity.getInstance().getPlatform().getPlatformType();
    }

    public static ISingularityExtension.ServerType getServerType() {
        return Singularity.getInstance().getPlatform().getServerType();
    }

    public static void sendResourcePack(CosmicResourcePack resourcePack, CosmicPlayer player) {
        Singularity.getInstance().getPlatform().sendResourcePack(resourcePack, player);
    }

    public static double getPlayerPing(String uuid) {
        return Singularity.getInstance().getUserManager().getPlayerPing(uuid);
    }

    public static ClassLoader getMainClassLoader() {
        return Singularity.getInstance().getPlatform().getMainClassLoader();
    }

    public static String parseOnProxy(CosmicSender user, String toParse) {
        return MessageUtils.parseOnProxy(user, toParse);
    }

    public static String parseOnProxy(String toParse) {
        return MessageUtils.parseOnProxy(UserUtils.getConsole(), toParse);
    }

    public static void kick(CosmicPlayer user, String message) {
        Singularity.getInstance().getUserManager().kick(user, message);
    }

    public static void kick(CosmicPlayer user) {
        Singularity.getInstance().getUserManager().kick(user, "&cConnection Closed by Server");
    }

    /**
     * @deprecated Use {@link UserUtils#teleport(CosmicSender, CosmicPlayer)} instead.
     * @param player The player to teleport.
     * @param target The target player to teleport to.
     */
    @Deprecated(since = "2.5.5.0")
    public static void teleport(CosmicPlayer player, CosmicPlayer target) {
        UserUtils.teleport(player, target);
    }

    public static void teleport(CosmicSender player, CosmicPlayer target) {
        UserUtils.teleport(player, target);
    }

    public static void teleport(CosmicSender player, CosmicLocation location) {
        UserUtils.teleport(player, location);
    }

    public static String replacePlaceholders(String string) {
        return RATRegistry.fetchDirty(string);
    }

    public static String replacePlaceholders(CosmicSender user, String string) {
        return RATRegistry.fetchDirty(string, user);
    }
}
