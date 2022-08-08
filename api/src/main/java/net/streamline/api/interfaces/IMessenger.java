package net.streamline.api.interfaces;

import com.mongodb.lang.Nullable;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public interface IMessenger {
    void logInfo(String message);

    void logWarning(String message);

    void logSevere(String message);

    String loggedModulePrefix(StreamlineModule module);

    void logInfo(StreamlineModule module, String message);

    void logWarning(StreamlineModule module, String message);

    void logSevere(StreamlineModule module, String message);

    void sendMessage(@Nullable StreamlineUser to, String message);

    void sendMessage(@Nullable StreamlineUser to, String otherUUID, String message);

    void sendMessage(@Nullable StreamlineUser to, StreamlineUser other, String message);

    void sendMessage(String to, String message);

    void sendMessage(@Nullable String to, String otherUUID, String message);

    void sendTitle(StreamlinePlayer user, StreamlineTitle title);

    String getListAsFormattedString(List<?> list);

    String removeExtraDot(String string);

    String resize(String text, int digits);

    String truncate(String text, int digits);

    int getDigits(int start, int otherSize);

    TreeSet<String> getCompletion(List<String> of, String param);

    TreeSet<String> getCompletion(TreeSet<String> of, String param);

    String stripColor(String string);

    String[] argsMinus(String[] args, int... toRemove);

    String argsToStringMinus(String[] args, int... toRemove);

    String argsToString(String[] args);

    String codedString(String text);

    String formatted(String string);

    String isolateChatColor(String format);

    String newLined(String text);

    boolean isCommand(String msg);

    String normalize(String[] splitMsg);

    String normalize(TreeSet<String> splitMsg);

    String normalize(TreeMap<Integer, String> splitMsg);

    boolean equalsAll(Object object, Object... toEqual);

    boolean equalsAll(Object object, Collection<Object> toEqual);

    boolean equalsAny(Object object, Collection<?> toEqual);

    String replaceAllPlayerBungee(StreamlineUser user, String of);
    
    String replaceAllPlayerBungee(String uuid, String of);

    List<String> getStringListFromString(String string);

    boolean isNullOrLessThanEqualTo(Object[] thingArray, int lessThanOrEqualTo);
}
