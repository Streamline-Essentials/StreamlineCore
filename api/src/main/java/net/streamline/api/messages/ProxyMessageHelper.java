package net.streamline.api.messages;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.interfaces.IStreamline;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MatcherUtils;
import net.streamline.api.utils.MessageUtils;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class ProxyMessageHelper {
    public static SingleSet<String, String> extrapolate(String from) {
        List<String[]> groups = MatcherUtils.getGroups(MatcherUtils.matcherBuilder("(.+)[=](.+)[;]", from), 2);
        if (groups.isEmpty()) return new SingleSet<>("", "");

        String[] strings = groups.get(0);
        return new SingleSet<>(strings[0], strings[1]);
    }

    public static byte[] removeSubChannel(byte[] data) {
        byte[] newData = new byte[data.length - 1];
        for (int i = 1; i < data.length; i ++) {
            newData[i - 1] = data[i];
        }
        return newData;
    }

    public static byte[] getSubChannel(String subChannel) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        output.writeUTF(subChannel);

        return output.toByteArray();
    }

    public static byte[] concat(byte[]... arrays) {
        List<Byte> list = new ArrayList<>();

        for (byte[] array : arrays) {
            for (byte b : array) {
                list.add(b);
            }
        }

        byte[] r = new byte[list.size()];

        for (int i = 0; i < list.size(); i ++) {
            r[i] = list.get(i);
        }

        return r;
    }

    @Getter @Setter
    private static ConcurrentSkipListMap<Date, SingleSet<String, String>> cachedQueries = new ConcurrentSkipListMap<>();

    @Getter @Setter
    private static ConcurrentSkipListMap<Date, ReturnableMessage> pendingMessages = new ConcurrentSkipListMap<>();

    @Getter @Setter
    private static ConcurrentSkipListMap<String, ConcurrentSkipListSet<Date>> pendingPlayers = new ConcurrentSkipListMap<>();

    public static Date addPendingMessage(ReturnableMessage returnableMessage) {
        Date r = new Date();
        getPendingMessages().put(r, returnableMessage);
        return r;
    }

    public static Date getDateOfPendingMessage(ReturnableMessage returnableMessage) {
        for (Date date : getPendingMessages().keySet()) {
            if (getPendingMessages().get(date).equals(returnableMessage)) return date;
        }

        return null;
    }

    public static void addPendingPlayer(StreamlineUser user, ReturnableMessage message) {
        ConcurrentSkipListSet<Date> r = getPendingDatesOfPlayer(user.getUuid());
        r.add(addPendingMessage(message));
        getPendingPlayers().put(user.getUuid(), r);
    }

    public static ConcurrentSkipListSet<Date> getPendingDatesOfPlayer(String uuid) {
        ConcurrentSkipListSet<Date> r = getPendingPlayers().get(uuid);
        if (r != null) return r;
        return new ConcurrentSkipListSet<>();
    }

    public static boolean containsQuery(String query) {
        for (Date date : getCachedQueries().keySet()) {
            if (getCachedQueries().get(date).key.equals(query)) return true;
        }

        return false;
    }

    public static void cacheQuery(String query, String result) {
        if (containsQuery(query)) {
            removeQuery(query);
        }
        getCachedQueries().put(new Date(), new SingleSet<>(query, result));
    }

    public static void removeQuery(String query) {
        if (! containsQuery(query)) return;

        for (Date date : getCachedQueries().keySet()) {
            if (getCachedQueries().get(date).key.equals(query)) getCachedQueries().remove(date);
        }
    }

    public static void removeQuery(Date date) {
        getCachedQueries().remove(date);
    }

    public static String getQuery(String key) {
        if (! containsQuery(key)) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_PENDING.get();

        for (Date date : getCachedQueries().keySet()) {
            SingleSet<String, String> r = getCachedQueries().get(date);
            if (r.key.equals(key)) return r.value;
        }

        return null;
    }

    public static String parseOnProxy(String toParse, StreamlineUser user) {
        if (SLAPI.getInstance().getPlatform().getServerType().equals(IStreamline.ServerType.PROXY)) return MessageUtils.replaceAllPlayerBungee(user, toParse);

        ReturnableQuery returnableQuery = new ReturnableQuery(toParse, user);
        addPendingPlayer(user, returnableQuery);

        return getQuery(toParse);
    }
}
