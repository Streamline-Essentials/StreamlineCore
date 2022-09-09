package net.streamline.api.modules;

import com.mongodb.lang.Nullable;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.StorageResource;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.messages.ProxyMessageIn;
import net.streamline.api.messages.ProxyMessageOut;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.profile.StreamlineProfiler;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.events.StreamlineListener;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.placeholder.RATAPI;
import net.streamline.api.savables.SavableResource;
import net.streamline.api.savables.users.OperatorUser;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.ModuleTaskManager;
import net.streamline.api.scheduler.TaskManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public class ModuleUtils {
    public static String loggedModulePrefix(StreamlineModule module) {
        return SLAPI.getInstance().getMessenger().loggedModulePrefix(module);
    }

    public static void logInfo(StreamlineModule module, String message) {
        SLAPI.getInstance().getMessenger().logInfo(module, message);
    }

    public static void logWarning(StreamlineModule module, String message) {
        SLAPI.getInstance().getMessenger().logWarning(module, message);
    }

    public static void logSevere(StreamlineModule module, String message) {
        SLAPI.getInstance().getMessenger().logSevere(module, message);
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
        SLAPI.getInstance().getMessenger().sendMessage(to, message);
    }

    public static void sendMessage(@Nullable String to, String otherUUID, String message) {
        SLAPI.getInstance().getMessenger().sendMessage(to, otherUUID, message);
    }

    public static void sendTitle(StreamlinePlayer user, StreamlineTitle title) {
        SLAPI.getInstance().getMessenger().sendTitle(user, title);
    }

    public static String getListAsFormattedString(List<?> list) {
        return SLAPI.getInstance().getMessenger().getListAsFormattedString(list);
    }

    public static String removeExtraDot(String string){
        return SLAPI.getInstance().getMessenger().removeExtraDot(string);
    }

    public static String resize(String text, int digits) {
        return SLAPI.getInstance().getMessenger().resize(text, digits);
    }

    public static String truncate(String text, int digits) {
        return SLAPI.getInstance().getMessenger().truncate(text, digits);
    }

    public static int getDigits(int start, int otherSize){
        return SLAPI.getInstance().getMessenger().getDigits(start, otherSize);
    }

    public static TreeSet<String> getCompletion(List<String> of, String param){
        return SLAPI.getInstance().getMessenger().getCompletion(of, param);
    }

    public static TreeSet<String> getCompletion(TreeSet<String> of, String param){
        return SLAPI.getInstance().getMessenger().getCompletion(of, param);
    }

    public static String stripColor(String string){
        return SLAPI.getInstance().getMessenger().stripColor(string);
    }

    public static String[] argsMinus(String[] args, int... toRemove) {
        return SLAPI.getInstance().getMessenger().argsMinus(args, toRemove);
    }

    public static String argsToStringMinus(String[] args, int... toRemove){
        return SLAPI.getInstance().getMessenger().argsToStringMinus(args, toRemove);
    }

    public static String argsToString(String[] args){
        return SLAPI.getInstance().getMessenger().argsToString(args);
    }

    public static String codedString(String text){
        return SLAPI.getInstance().getMessenger().codedString(text);
    }

    public static String formatted(String string) {
        return SLAPI.getInstance().getMessenger().formatted(string);
    }

    public static String isolateChatColor(String format) {
        return SLAPI.getInstance().getMessenger().isolateChatColor(format);
    }

    public static String newLined(String text){
        return SLAPI.getInstance().getMessenger().newLined(text);
    }

    public static boolean isCommand(String msg){
        return SLAPI.getInstance().getMessenger().isCommand(msg);
    }

    public static String normalize(String[] splitMsg){
        return SLAPI.getInstance().getMessenger().normalize(splitMsg);
    }

    public static String normalize(TreeSet<String> splitMsg) {
        return SLAPI.getInstance().getMessenger().normalize(splitMsg);
    }

    public static String normalize(TreeMap<Integer, String> splitMsg) {
        return SLAPI.getInstance().getMessenger().normalize(splitMsg);
    }

    public static boolean equalsAll(Object object, Object... toEqual){
        return SLAPI.getInstance().getMessenger().equalsAll(object, toEqual);
    }

    public static boolean equalsAll(Object object, Collection<Object> toEqual){
        return SLAPI.getInstance().getMessenger().equalsAll(object, toEqual);
    }

    public static boolean equalsAny(Object object, Collection<?> toEqual){
        return SLAPI.getInstance().getMessenger().equalsAny(object, toEqual);
    }

    public static String replaceAllPlayerBungee(StreamlineUser user, String of) {
        return SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(user, of);
    }

    public static String replaceAllPlayerBungee(String uuid, String of) {
        return SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(uuid, of);
    }

    public static List<String> getStringListFromString(String string) {
        return SLAPI.getInstance().getMessenger().getStringListFromString(string);
    }

    public static boolean isNullOrLessThanEqualTo(Object[] thingArray, int lessThanOrEqualTo) {
        return SLAPI.getInstance().getMessenger().isNullOrLessThanEqualTo(thingArray, lessThanOrEqualTo);
    }

    public static List<StreamlineUser> getLoadedUsers() {
        return SLAPI.getInstance().getUserManager().getLoadedUsers();
    }

    public static StreamlineUser loadUser(StreamlineUser user) {
        return SLAPI.getInstance().getUserManager().loadUser(user);
    }

    public static void unloadUser(StreamlineUser user) {
        SLAPI.getInstance().getUserManager().unloadUser(user);
    }

    public static boolean userExists(String uuid) {
        return SLAPI.getInstance().getUserManager().userExists(uuid);
    }

    public static StreamlineUser getOrGetUser(String uuid) {
        return SLAPI.getInstance().getUserManager().getOrGetUser(uuid);
    }

    public static StreamlinePlayer getOrGetPlayer(String uuid) {
        return SLAPI.getInstance().getUserManager().getOrGetPlayer(uuid);
    }

    public static StorageResource<?> newStorageResource(String uuid, Class<? extends SavableResource> clazz) {
        return SLAPI.getInstance().getUserManager().newStorageResource(uuid, clazz);
    }

    public static boolean isConsole(String uuid) {
        return SLAPI.getInstance().getUserManager().isConsole(uuid);
    }

    public static boolean isOnline(String uuid) {
        return SLAPI.getInstance().getUserManager().isOnline(uuid);
    }

    public static String getOffOnFormatted(StreamlineUser stat){
        return SLAPI.getInstance().getUserManager().getOffOnFormatted(stat);
    }

    public static String getOffOnAbsolute(StreamlineUser stat){
        return SLAPI.getInstance().getUserManager().getOffOnAbsolute(stat);
    }

    public static String getFormatted(StreamlineUser stat){
        return SLAPI.getInstance().getUserManager().getFormatted(stat);
    }

    public static String getAbsolute(StreamlineUser stat){
        return SLAPI.getInstance().getUserManager().getAbsolute(stat);
    }

    public static String getLuckPermsPrefix(String username){
        return SLAPI.getInstance().getUserManager().getLuckPermsPrefix(username);
    }

    public static String getLuckPermsSuffix(String username){
        return SLAPI.getInstance().getUserManager().getLuckPermsSuffix(username);
    }

    public static String getDisplayName(String username, String nickName) {
        return SLAPI.getInstance().getUserManager().getDisplayName(username, nickName);
    }

    public static void fireEvent(StreamlineEvent event) {
        SLAPI.getInstance().getPlatform().fireEvent(event);
    }

    public static void listen(StreamlineListener listener, StreamlineModule module) {
        ModuleManager.registerEvents(listener, module);
    }

    public static @NotNull Collection<StreamlinePlayer> getOnlinePlayers() {
        return SLAPI.getInstance().getPlatform().getOnlinePlayers();
    }

    public static List<String> getOnlinePlayerNames() {
        return SLAPI.getInstance().getPlatform().getOnlinePlayerNames();
    }

    public static boolean hasPermission(StreamlineUser user, String permission) {
        if (user.isBypassPermissions()) return true;
        return SLAPI.getInstance().getPlatform().hasPermission(user, permission);
    }

    public static LuckPerms getLuckPerms() {
        return SLAPI.getInstance().getPlatform().getLuckPerms();
    }

    public static RATAPI getRATAPI() {
        return SLAPI.getInstance().getPlatform().getRATAPI();
    }

    public static StreamlineConsole getConsole() {
        return SLAPI.getInstance().getUserManager().getConsole();
    }

    public static void addPermission(User user, String permission) {
        SLAPI.getInstance().getUserManager().addPermission(user, permission);
    }

    public static void removePermission(User user, String permission) {
        SLAPI.getInstance().getUserManager().removePermission(user, permission);
    }

    public static boolean runAs(OperatorUser user, String command) {
        return SLAPI.getInstance().getUserManager().runAs(user, command);
    }

    public static boolean runAs(StreamlineUser user, String command) {
        return SLAPI.getInstance().getUserManager().runAs(user, command);
    }

    public static boolean runAs(StreamlineUser user, boolean bypass, String command) {
        return SLAPI.getInstance().getUserManager().runAs(user, bypass, command);
    }

    public static String getUUIDFromName(String name) {
        return SLAPI.getInstance().getPlatform().getUUIDFromName(name);
    }

    public static StreamlineUser getOrGetUserByName(String name) {
        return SLAPI.getInstance().getUserManager().getOrGetUserByName(name);
    }

    public static void chatAs(StreamlineUser as, String message) {
        SLAPI.getInstance().getPlatform().chatAs(as, message);
    }

    public static void runAsStrictly(StreamlineUser as, String message) {
        SLAPI.getInstance().getPlatform().runAsStrictly(as, message);
    }

    public static List<StreamlineUser> getUsersOn(String server) {
        return SLAPI.getInstance().getUserManager().getUsersOn(server);
    }

    public static List<String> getServerNames() {
        return SLAPI.getInstance().getPlatform().getServerNames();
    }

    public static void connect(StreamlineUser user, String server) {
        SLAPI.getInstance().getUserManager().connect(user, server);
    }

    public static boolean isGeyserPlayer(StreamlineUser user) {
        return SLAPI.getInstance().getUserManager().isGeyserPlayer(user);
    }

    public static boolean isGeyserPlayer(String uuid) {
        return SLAPI.getInstance().getUserManager().isGeyserPlayer(uuid);
    }

    public static boolean serverHasPlugin(String plugin) {
        return SLAPI.getInstance().getPlatform().serverHasPlugin(plugin);
    }

    public static StreamlineServerInfo getStreamlineServer(String server) {
        return SLAPI.getInstance().getPlatform().getStreamlineServer(server);
    }

    public static StreamlineProfiler getProfiler() {
        return SLAPI.getInstance().getPlatform().getProfiler();
    }

    public static boolean equalsAnyServer(String servername) {
        return SLAPI.getInstance().getPlatform().equalsAnyServer(servername);
    }

    public static ModuleTaskManager getModuleScheduler() {
        return SLAPI.getInstance().getModuleScheduler();
    }

    public static TaskManager getMainScheduler() {
        return SLAPI.getInstance().getMainScheduler();
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
}
