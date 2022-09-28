package net.streamline.api.interfaces;

import net.luckperms.api.model.user.User;
import net.streamline.api.configs.StorageResource;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.savables.SavableResource;
import net.streamline.api.savables.users.OperatorUser;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public interface IUserManager {
    String getUsername(String uuid);

    boolean isOnline(String uuid);

    boolean runAs(StreamlineUser user, boolean bypass, String command);

    ConcurrentSkipListSet<StreamlineUser> getUsersOn(String server);

    void connect(StreamlineUser user, String server);

    void sendUserResourcePack(StreamlineUser user, StreamlineResourcePack pack);

    String parsePlayerIP(String uuid);

    double getPlayerPing(String uuid);
}
