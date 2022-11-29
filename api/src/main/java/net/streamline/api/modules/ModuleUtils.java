package net.streamline.api.modules;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.messages.builders.TeleportMessageBuilder;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.placeholders.RATRegistry;
import net.streamline.api.profile.StreamlineProfiler;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.*;
import net.streamline.api.scheduler.ModuleTaskManager;
import net.streamline.api.scheduler.TaskManager;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import org.jetbrains.annotations.Nullable;
import tv.quaint.events.BaseEventListener;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class ModuleUtils {
    public static String loggedModulePrefix(StreamlineModule module) {
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

    public static void sendMessage(@Nullable StreamlineUser to, String message) {
        SLAPI.getInstance().getMessenger().sendMessage(to, message);
    }

    public static void sendMessage(@Nullable StreamlineUser to, String otherUUID, String message) {
        SLAPI.getInstance().getMessenger().sendMessage(to, otherUUID, message);
    }

    public static void sendMessage(@Nullable StreamlineUser to, StreamlineUser other, String message) {
        SLAPI.getInstance().getMessenger().sendMessage(to, other, message);
    }

    public static void sendMessage(String to, String message) {
        MessageUtils.sendMessage(to, message);
    }

    public static void sendMessage(@Nullable String to, String otherUUID, String message) {
        MessageUtils.sendMessage(to, otherUUID, message);
    }

    public static void sendTitle(StreamlinePlayer user, StreamlineTitle title) {
        SLAPI.getInstance().getMessenger().sendTitle(user, title);
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
        return SLAPI.getInstance().getMessenger().stripColor(string);
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

    public static String replaceAllPlayerBungee(StreamlineUser user, String of) {
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

    public static ConcurrentSkipListMap<String, StreamlineUser> getLoadedUsers() {
        return UserUtils.getLoadedUsers();
    }

    public static ConcurrentSkipListMap<String, StreamlinePlayer> getLoadedPlayers() {
        return UserUtils.getLoadedPlayers();
    }

    public static ConcurrentSkipListMap<String, StreamlineUser> getOnlineUsers() {
        return UserUtils.getOnlineUsers();
    }

    public static ConcurrentSkipListMap<String, StreamlinePlayer> getOnlinePlayers() {
        return UserUtils.getOnlinePlayers();
    }

    public static ConcurrentSkipListSet<StreamlineUser> getLoadedUsersSet() {
        return UserUtils.getLoadedUsersSet();
    }

    public static StreamlineUser loadUser(StreamlineUser user) {
        return UserUtils.loadUser(user);
    }

    public static void unloadUser(StreamlineUser user) {
        UserUtils.unloadUser(user);
    }

    public static boolean userExists(String uuid) {
        return UserUtils.userExists(uuid);
    }

    public static StreamlineUser getOrGetUser(String uuid) {
        return UserUtils.getOrGetUser(uuid);
    }

    public static StreamlinePlayer getOrGetPlayer(String uuid) {
        return UserUtils.getOrGetPlayer(uuid);
    }

    public static boolean isConsole(String uuid) {
        return UserUtils.isConsole(uuid);
    }

    public static boolean isOnline(String uuid) {
        return SLAPI.getInstance().getUserManager().isOnline(uuid);
    }

    public static String getOffOnFormatted(StreamlineUser stat){
        return UserUtils.getOffOnFormatted(stat);
    }

    public static String getOffOnAbsolute(StreamlineUser stat){
        return UserUtils.getOffOnAbsolute(stat);
    }

    public static String getFormatted(StreamlineUser stat){
        return UserUtils.getFormatted(stat);
    }

    public static String getAbsolute(StreamlineUser stat){
        return UserUtils.getAbsolute(stat);
    }

    public static String getLuckPermsPrefix(String username){
        return UserUtils.getLuckPermsPrefix(username);
    }

    public static String getLuckPermsSuffix(String username){
        return UserUtils.getLuckPermsSuffix(username);
    }

    public static String getDisplayName(StreamlineUser user) {
        return UserUtils.getFormattedDefaultNickname(user);
    }

    public static void fireEvent(StreamlineEvent event) {
        ModuleManager.fireEvent(event);
    }

    public static void listen(BaseEventListener listener, StreamlineModule module) {
        ModuleManager.registerEvents(listener, module);
    }

    public static ConcurrentSkipListSet<String> getOnlinePlayerNames() {
        return SLAPI.getInstance().getPlatform().getOnlinePlayerNames();
    }

    public static boolean hasPermission(StreamlineUser user, String permission) {
        if (user.isBypassPermissions()) return true;
        return SLAPI.getInstance().getPlatform().hasPermission(user, permission);
    }

    public static LuckPerms getLuckPerms() {
        return SLAPI.getLuckPerms();
    }

    public static StreamlineConsole getConsole() {
        return UserUtils.getConsole();
    }

    public static void addPermission(User user, String permission) {
        UserUtils.addPermission(user, permission);
    }

    public static void removePermission(User user, String permission) {
        UserUtils.removePermission(user, permission);
    }

    public static boolean runAs(OperatorUser user, String command) {
        return UserUtils.runAs(user, command);
    }

    public static boolean runAs(StreamlineUser user, String command) {
        return UserUtils.runAs(user, command);
    }

    public static boolean runAs(StreamlineUser user, boolean bypass, String command) {
        return SLAPI.getInstance().getUserManager().runAs(user, bypass, command);
    }

    public static String getUUIDFromName(String name) {
        return UserUtils.getUUIDFromName(name);
    }

    public static StreamlineUser getOrGetUserByName(String name) {
        return UserUtils.getOrGetUserByName(name);
    }

    public static void chatAs(StreamlineUser as, String message) {
        SLAPI.getInstance().getPlatform().chatAs(as, message);
    }

    public static void runAsStrictly(StreamlineUser as, String message) {
        SLAPI.getInstance().getPlatform().runAsStrictly(as, message);
    }

    public static ConcurrentSkipListSet<StreamlineUser> getUsersOn(String server) {
        return SLAPI.getInstance().getUserManager().getUsersOn(server);
    }

    public static ConcurrentSkipListSet<String> getServerNames() {
        return SLAPI.getInstance().getPlatform().getServerNames();
    }

    public static void connect(StreamlineUser user, String server) {
        SLAPI.getInstance().getUserManager().connect(user, server);
    }

    public static boolean isGeyserPlayer(StreamlineUser user) {
        return UserUtils.isGeyserPlayer(user);
    }

    public static boolean isGeyserPlayer(String uuid) {
        return UserUtils.isGeyserPlayer(uuid);
    }

    public static boolean serverHasPlugin(String plugin) {
        return SLAPI.getInstance().getPlatform().serverHasPlugin(plugin);
    }

    public static StreamlineProfiler getProfiler() {
        return SLAPI.getInstance().getProfiler();
    }

    public static boolean equalsAnyServer(String servername) {
        return SLAPI.getInstance().getPlatform().equalsAnyServer(servername);
    }

    public static ModuleTaskManager getModuleScheduler() {
        return SLAPI.getModuleScheduler();
    }

    public static TaskManager getMainScheduler() {
        return SLAPI.getMainScheduler();
    }

    public static IStreamline.PlatformType getPlatformType() {
        return SLAPI.getInstance().getPlatform().getPlatformType();
    }

    public static IStreamline.ServerType getServerType() {
        return SLAPI.getInstance().getPlatform().getServerType();
    }

    public static void sendResourcePack(StreamlineResourcePack resourcePack, StreamlineUser player) {
        SLAPI.getInstance().getPlatform().sendResourcePack(resourcePack, player);
    }

    public static double getPlayerPing(String uuid) {
        return SLAPI.getInstance().getUserManager().getPlayerPing(uuid);
    }

    public static ClassLoader getMainClassLoader() {
        return SLAPI.getInstance().getPlatform().getMainClassLoader();
    }

    public static void setStreamlineServer(StreamlineServerInfo serverInfo) {
        GivenConfigs.getProfileConfig().updateServerInfo(serverInfo);
    }

    public static StreamlineServerInfo getServerInfo(String server) {
        return GivenConfigs.getProfileConfig().getServerInfo(server);
    }

    public static String parseOnProxy(StreamlineUser user, String toParse) {
        return MessageUtils.parseOnProxy(user, toParse);
    }

    public static String parseOnProxy(String toParse) {
        return MessageUtils.parseOnProxy(toParse);
    }

    public static void kick(StreamlineUser user, String message) {
        SLAPI.getInstance().getUserManager().kick(user, message);
    }

    public static void kick(StreamlineUser user) {
        SLAPI.getInstance().getUserManager().kick(user, "&cConnection Closed by Server");
    }

    public static void teleport(StreamlinePlayer player, StreamlineLocation location) {
        TeleportMessageBuilder.build(player, location, player).send();
    }

    public static void teleport(StreamlinePlayer player, StreamlinePlayer target) {
        teleport(player, target.getLocation());
    }

    public static String replacePlaceholders(String string) {
        return RATRegistry.fetchDirty(string);
    }

    public static String replacePlaceholders(StreamlineUser user, String string) {
        return RATRegistry.fetchDirty(string, user);
    }
}
