package net.streamline.api.interfaces;

import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.concurrent.ConcurrentSkipListSet;

public interface IUserManager {
    String getUsername(String uuid);

    boolean isOnline(String uuid);

    boolean runAs(StreamlineUser user, boolean bypass, String command);

    ConcurrentSkipListSet<StreamlineUser> getUsersOn(String server);

    void connect(StreamlineUser user, String server);

    void kick(StreamlineUser user, String message);

    void sendUserResourcePack(StreamlineUser user, StreamlineResourcePack pack);

    String parsePlayerIP(String uuid);

    double getPlayerPing(String uuid);
}
