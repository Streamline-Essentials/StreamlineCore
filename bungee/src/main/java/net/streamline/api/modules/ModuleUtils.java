package net.streamline.api.modules;

import com.mongodb.lang.Nullable;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.configs.StorageResource;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.events.StreamlineEventBus;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.SavableResource;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.base.Streamline;
import net.streamline.utils.MessagingUtils;

import java.util.*;
import java.util.List;

public class ModuleUtils {
    public static String loggedModulePrefix(BundledModule module) {
        return MessagingUtils.loggedModulePrefix(module);
    }

    public static void logInfo(BundledModule module, String message) {
        MessagingUtils.logInfo(module, message);
    }

    public static void logWarning(BundledModule module, String message) {
        MessagingUtils.logWarning(module, message);
    }

    public static void logSevere(BundledModule module, String message) {
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

    public static java.util.List<String> getStringListFromString(String string) {
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

    public static SavablePlayer getOrGetPlayer(ProxiedPlayer player) {
        return UserManager.getOrGetPlayer(player);
    }

    public static SavablePlayer getOrGetPlayer(String uuid) {
        return UserManager.getOrGetPlayer(uuid);
    }

    public static SavableUser getOrGetUser(CommandSender sender) {
        return UserManager.getOrGetUser(sender);
    }

    public static StorageResource<?> newStorageResource(String uuid, Class<? extends SavableResource> clazz) {
        return UserManager.newStorageResource(uuid, clazz);
    }

    public static String getUsername(CommandSender sender) {
        return UserManager.getUsername(sender);
    }

    public static boolean isConsole(CommandSender sender) {
        return UserManager.isConsole(sender);
    }

    public static boolean isConsole(String uuid) {
        return UserManager.isConsole(uuid);
    }

    public static boolean isOnline(String uuid) {
        return UserManager.isOnline(uuid);
    }

    public static String parsePlayerIP(ProxiedPlayer player) {
        return UserManager.parsePlayerIP(player);
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

    public static void fireEvent(StreamlineEvent<?> event) {
        Streamline.getInstance().getProxy().getPluginManager().callEvent(event);
    }

    public static void listen(StreamlineEventBus.StreamlineObserver observer) {
        Streamline.getStreamlineEventBus().addObserver(observer);
    }
}
