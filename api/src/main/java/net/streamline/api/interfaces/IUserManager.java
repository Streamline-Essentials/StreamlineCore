package net.streamline.api.interfaces;

import net.luckperms.api.model.user.User;
import net.streamline.api.configs.StorageResource;
import net.streamline.api.savables.SavableResource;
import net.streamline.api.savables.users.OperatorUser;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.List;

public interface IUserManager {
    List<StreamlineUser> getLoadedUsers();

    StreamlineUser loadUser(StreamlineUser user);

    void unloadUser(StreamlineUser user);

    boolean userExists(String uuid);

    StreamlineUser getOrGetUser(String uuid);

    StreamlinePlayer getOrGetPlayer(String uuid);

    StorageResource<?> newStorageResource(String uuid, Class<? extends SavableResource> clazz);

    boolean isConsole(String uuid);

    boolean isOnline(String uuid);

    String getOffOnFormatted(StreamlineUser stat);

    String getOffOnAbsolute(StreamlineUser stat);

    String getFormatted(StreamlineUser stat);

    String getAbsolute(StreamlineUser stat);

    String getLuckPermsPrefix(String username);

    String getLuckPermsSuffix(String username);

    String getDisplayName(String username, String nickName);

    StreamlineConsole getConsole();

    void addPermission(User user, String permission);

    void removePermission(User user, String permission);

    boolean runAs(OperatorUser user, String command);

    boolean runAs(StreamlineUser user, String command);

    boolean runAs(StreamlineUser user, boolean bypass, String command);
    
    StreamlineUser getOrGetUserByName(String name);

    List<StreamlineUser> getUsersOn(String server);
    
    void connect(StreamlineUser user, String server);

    boolean isGeyserPlayer(StreamlineUser user);

    boolean isGeyserPlayer(String uuid);
}
