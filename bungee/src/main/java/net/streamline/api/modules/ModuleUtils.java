package net.streamline.api.modules;

import com.mongodb.lang.Nullable;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.streamline.api.configs.StorageResource;
import net.streamline.api.events.ProperEvent;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.events.StreamlineListener;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.placeholder.RATAPI;
import net.streamline.api.savables.SavableResource;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.OperatorUser;
import net.streamline.api.savables.users.SavableConsole;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.base.Streamline;
import net.streamline.utils.MessagingUtils;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public class ModuleUtils {
    public static String loggedModulePrefix(StreamlineModule module) {
        return MessagingUtils.loggedModulePrefix(module);
    }

    public static void logInfo(StreamlineModule module, String message) {
        MessagingUtils.logInfo(module, message);
    }

    public static void logWarning(StreamlineModule module, String message) {
        MessagingUtils.logWarning(module, message);
    }

    public static void logSevere(StreamlineModule module, String message) {
        MessagingUtils.logSevere(module, message);
    }

    public static void sendMessage(@Nullable SavableUser to, String message) {
        MessagingUtils.sendMessage(to, message);
    }

    public static void sendMessage(@Nullable SavableUser to, String otherUUID, String message) {
        MessagingUtils.sendMessage(to, otherUUID, message);
    }

    public static void sendMessage(@Nullable SavableUser to, SavableUser other, String message) {
        MessagingUtils.sendMessage(to, other, message);
    }

    public static void sendMessage(String to, String message) {
        MessagingUtils.sendMessage(to, message);
    }

    public static void sendMessage(@Nullable String to, String otherUUID, String message) {
        MessagingUtils.sendMessage(to, otherUUID, message);
    }

    public static void sendTitle(SavablePlayer user, StreamlineTitle title) {
        MessagingUtils.sendTitle(user, title);
    }

    public static String getListAsFormattedString(List<?> list) {
        return MessagingUtils.getListAsFormattedString(list);
    }

    public static String removeExtraDot(String string){
        return MessagingUtils.removeExtraDot(string);
    }

    public static String resize(String text, int digits) {
        return MessagingUtils.resize(text, digits);
    }

    public static String truncate(String text, int digits) {
        return MessagingUtils.truncate(text, digits);
    }

    public static int getDigits(int start, int otherSize){
        return MessagingUtils.getDigits(start, otherSize);
    }

    public static TreeSet<String> getCompletion(List<String> of, String param){
        return MessagingUtils.getCompletion(of, param);
    }

    public static TreeSet<String> getCompletion(TreeSet<String> of, String param){
        return MessagingUtils.getCompletion(of, param);
    }

    public static String stripColor(String string){
        return MessagingUtils.stripColor(string);
    }

    public static String[] argsMinus(String[] args, int... toRemove) {
        return MessagingUtils.argsMinus(args, toRemove);
    }

    public static String argsToStringMinus(String[] args, int... toRemove){
        return MessagingUtils.argsToStringMinus(args, toRemove);
    }

    public static String argsToString(String[] args){
        return MessagingUtils.argsToString(args);
    }

    public static String codedString(String text){
        return MessagingUtils.codedString(text);
    }

    public static String formatted(String string) {
        return MessagingUtils.formatted(string);
    }

    public static String isolateChatColor(String format) {
        return MessagingUtils.isolateChatColor(format);
    }

    public static String newLined(String text){
        return MessagingUtils.newLined(text);
    }

    public static boolean isCommand(String msg){
        return MessagingUtils.isCommand(msg);
    }

    public static String normalize(String[] splitMsg){
        return MessagingUtils.normalize(splitMsg);
    }

    public static String normalize(TreeSet<String> splitMsg) {
        return MessagingUtils.normalize(splitMsg);
    }

    public static String normalize(TreeMap<Integer, String> splitMsg) {
        return MessagingUtils.normalize(splitMsg);
    }

    public static boolean equalsAll(Object object, Object... toEqual){
        return MessagingUtils.equalsAll(object, toEqual);
    }

    public static boolean equalsAll(Object object, Collection<Object> toEqual){
        return MessagingUtils.equalsAll(object, toEqual);
    }

    public static boolean equalsAny(Object object, Collection<?> toEqual){
        return MessagingUtils.equalsAny(object, toEqual);
    }

    public static String replaceAllPlayerBungee(SavableUser user, String of) {
        return MessagingUtils.replaceAllPlayerBungee(user, of);
    }

    public static String replaceAllPlayerBungee(String uuid, String of) {
        return MessagingUtils.replaceAllPlayerBungee(uuid, of);
    }

    public static boolean equalsAnyServer(String servername) {
        return MessagingUtils.equalsAnyServer(servername);
    }

    public static List<String> getStringListFromString(String string) {
        return MessagingUtils.getStringListFromString(string);
    }

    public static boolean isNullOrLessThanEqualTo(Object[] thingArray, int lessThanOrEqualTo) {
        return MessagingUtils.isNullOrLessThanEqualTo(thingArray, lessThanOrEqualTo);
    }



    public static List<SavableUser> getLoadedUsers() {
        return UserManager.getLoadedUsers();
    }

    public static SavableUser loadUser(SavableUser user) {
        return UserManager.loadUser(user);
    }

    public static void unloadUser(SavableUser user) {
        UserManager.unloadUser(user);
    }

    public static SavableUser getOrGetUser(String uuid) {
        return UserManager.getOrGetUser(uuid);
    }

    public static SavablePlayer getOrGetPlayer(String uuid) {
        return UserManager.getOrGetPlayer(uuid);
    }

    public static StorageResource<?> newStorageResource(String uuid, Class<? extends SavableResource> clazz) {
        return UserManager.newStorageResource(uuid, clazz);
    }

    public static boolean isConsole(String uuid) {
        return UserManager.isConsole(uuid);
    }

    public static boolean isOnline(String uuid) {
        return UserManager.isOnline(uuid);
    }

    public static String getOffOnFormatted(SavableUser stat){
        return UserManager.getOffOnFormatted(stat);
    }

    public static String getOffOnAbsolute(SavableUser stat){
        return UserManager.getOffOnAbsolute(stat);
    }

    public static String getFormatted(SavableUser stat){
        return UserManager.getFormatted(stat);
    }

    public static String getAbsolute(SavableUser stat){
        return UserManager.getAbsolute(stat);
    }

    public static String getLuckPermsPrefix(String username){
        return UserManager.getLuckPermsPrefix(username);
    }

    public static String getLuckPermsSuffix(String username){
        return UserManager.getLuckPermsSuffix(username);
    }

    public static String getDisplayName(String username, String nickName) {
        return UserManager.getDisplayName(username, nickName);
    }

    public static void fireEvent(StreamlineEvent event) {
        Streamline.fireEvent(new ProperEvent(event));
    }

    public static void listen(StreamlineListener listener, StreamlineModule module) {
        ModuleManager.registerEvents(listener, module);
    }

    public static List<String> getOnlinePlayerNames() {
        return Streamline.getOnlinePlayerNames();
    }

    public static boolean hasPermission(SavableUser user, String permission) {
        if (user.isBypassPermissions()) return true;
        return Streamline.hasPermission(user, permission);
    }

    public static LuckPerms getLuckPerms() {
        return Streamline.getLuckPerms();
    }

    public static RATAPI getRATAPI() {
        return Streamline.getRATAPI();
    }

    public static SavableConsole getConsole() {
        return UserManager.getConsole();
    }

    public static void addPermission(User user, String permission) {
        UserManager.addPermission(user, permission);
    }

    public static void removePermission(User user, String permission) {
        UserManager.removePermission(user, permission);
    }

    public static boolean runAs(OperatorUser user, String command) {
        return UserManager.runAs(user, command);
    }

    public static boolean runAs(SavableUser user, String command) {
        return UserManager.runAs(user, command);
    }

    public static boolean runAs(SavableUser user, boolean bypass, String command) {
        return UserManager.runAs(user, bypass, command);
    }

    public static String getUUIDFromName(String name) {
        return Streamline.getUUIDFromName(name);
    }

    public static SavableUser getOrGetUserByName(String name) {
        return UserManager.getOrGetUserByName(name);
    }

    public static void chatAs(SavableUser as, String message) {
        Streamline.chatAs(as, message);
    }

    public static void runAsStrictly(SavableUser as, String message) {
        Streamline.runAs(as, message);
    }

    public static List<SavableUser> getUsersOn(String server) {
        return UserManager.getUsersOn(server);
    }

    public static List<String> getServerNames() {
        return Streamline.getServerNames();
    }

    public static void connect(SavableUser user, String server) {
        UserManager.connect(user, server);
    }
}
