package net.streamline.api.interfaces;

import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.players.location.PlayerLocation;
import net.streamline.api.objects.StreamlineResourcePack;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public interface IUserManager<C, P extends C> {
    StreamPlayer getOrCreatePlayer(P player);

    StreamSender getOrCreateSender(C sender);

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

    String getServerPlayerIsOn(P player);

    String getDisplayName(String uuid);

    P getPlayer(String uuid);

    ConcurrentSkipListMap<String, StreamPlayer> ensurePlayers();

    void teleport(StreamPlayer player, PlayerLocation location);
}
