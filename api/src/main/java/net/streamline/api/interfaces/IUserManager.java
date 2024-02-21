package net.streamline.api.interfaces;

import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.objects.StreamlineResourcePack;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public interface IUserManager<T> {
    String getUsername(String uuid);

    boolean isOnline(String uuid);

    boolean runAs(StreamPlayer user, boolean bypass, String command);

    ConcurrentSkipListSet<StreamPlayer> getUsersOn(String server);

    void connect(StreamPlayer user, String server);

    void kick(StreamPlayer user, String message);

    void sendUserResourcePack(StreamPlayer user, StreamlineResourcePack pack);

    String parsePlayerIP(String uuid);

    double getPlayerPing(String uuid);

    String getServerPlayerIsOn(String uuid);

    String getDisplayName(String uuid);

    T getPlayer(String uuid);

    ConcurrentSkipListMap<String, StreamPlayer> ensurePlayers();
}
