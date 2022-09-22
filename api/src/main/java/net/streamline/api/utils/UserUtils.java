package net.streamline.api.utils;

import de.leonhard.storage.Config;
import de.leonhard.storage.Json;
import de.leonhard.storage.Toml;
import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.*;
import net.streamline.api.configs.given.CachedUUIDsHandler;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.messages.builders.ResourcePackMessageBuilder;
import net.streamline.api.messages.builders.ServerConnectMessageBuilder;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.SavableResource;
import net.streamline.api.savables.events.LoadStreamlineUserEvent;
import net.streamline.api.savables.users.OperatorUser;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class UserUtils {
//    @Getter
//    private static UserUtils instance;
//
//    public static UserUtils() {
//        instance = this;
//    }

    @Getter @Setter
    private static ConcurrentSkipListMap<String, StreamlineUser> loadedUsers = new ConcurrentSkipListMap<>();

    public static ConcurrentSkipListSet<StreamlineUser> getLoadedUsersSet() {
        return new ConcurrentSkipListSet<>(getLoadedUsers().values());
    }

    public static StreamlineUser loadUser(StreamlineUser user) {
        getLoadedUsers().put(user.getUuid(), user);
        ModuleUtils.fireEvent(new LoadStreamlineUserEvent<>(user));
        return user;
    }

    public static void unloadUser(StreamlineUser user) {
        user.saveAll();
        getLoadedUsers().remove(user.getUuid());
    }

    private static StreamlineUser getUser(String uuid) {
        return getLoadedUsers().get(uuid);
    }

    public static boolean userExists(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().userConsoleDiscriminator())) return getLoadedUsersSet().contains(getConsole());
        StorageUtils.StorageType type = GivenConfigs.getMainConfig().userUseType();
        File userFolder = SLAPI.getUserFolder();
        switch (type) {
            case YAML -> {
                File[] files = userFolder.listFiles();
                if (files == null) return false;

                for (File file : files) {
                    if (file.getName().equals(uuid + ".yml")) return true;
                }
                return false;
            }
            case JSON -> {
                File[] files = userFolder.listFiles();
                if (files == null) return false;

                for (File file : files) {
                    if (file.getName().equals(uuid + ".json")) return true;
                }
                return false;
            }
            case TOML -> {
                File[] files = userFolder.listFiles();
                if (files == null) return false;

                for (File file : files) {
                    if (file.getName().equals(uuid + ".toml")) return true;
                }
                return false;
            }
            case MONGO -> {
                return GivenConfigs.getMainConfig().getConfiguredDatabase().mongoConnection().exists(
                        StreamlinePlayer.class.getSimpleName(),
                        StorageUtils.getWhere("uuid", uuid)
                );
            }
            case MYSQL -> {
                return GivenConfigs.getMainConfig().getConfiguredDatabase().mySQLConnection().exists(
                        new SQLCollection(StreamlinePlayer.class.getSimpleName(),
                                "uuid",
                                uuid
                        )
                );
            }
            default -> {
                return false;
            }
        }
    }

    public static StreamlineUser getOrGetUser(String uuid) {
        StreamlineUser user = getUser(uuid);
        if (user != null) return user;

        if (isConsole(uuid)) {
            user = new StreamlineConsole();
        } else {
            if (CachedUUIDsHandler.isCached(uuid) || userExists(uuid)) {
                user = new StreamlinePlayer(uuid);
            } else {
                return null;
            }
        }

        loadUser(user);
        return user;
    }

    @NotNull
    public static StreamlineUser getOrGetOrGetUser(String uuid) {
        StreamlineUser user = getOrGetUser(uuid);
        if (user != null) return user;

        if (isConsole(uuid)) {
            user = new StreamlineConsole();
        } else {
            user = new StreamlinePlayer(uuid);
        }

        loadUser(user);
        return user;
    }

    public static StreamlinePlayer getOrGetPlayer(String uuid) {
        StreamlineUser user = getOrGetUser(uuid);
        if (! (user instanceof StreamlinePlayer StreamlinePlayer)) return null;

        return StreamlinePlayer;
    }

    public static StorageResource<?> newStorageResource(String uuid, Class<? extends SavableResource> clazz) {
        switch (GivenConfigs.getMainConfig().userUseType()) {
            case YAML -> {
                return new FlatFileResource<>(Config.class, uuid + ".yml", SLAPI.getUserFolder(), false);
            }
            case JSON -> {
                return new FlatFileResource<>(Json.class, uuid + ".json", SLAPI.getUserFolder(), false);
            }
            case TOML -> {
                return new FlatFileResource<>(Toml.class, uuid + ".toml", SLAPI.getUserFolder(), false);
            }
            case MONGO -> {
                return new MongoResource(GivenConfigs.getMainConfig().getConfiguredDatabase(), clazz.getSimpleName(), "uuid", uuid);
            }
            case MYSQL -> {
                return new MySQLResource(GivenConfigs.getMainConfig().getConfiguredDatabase(), new SQLCollection(clazz.getSimpleName(), "uuid", uuid));
            }
        }

        return null;
    }

    public static boolean isConsole(String uuid) {
        return uuid.equals(GivenConfigs.getMainConfig().userConsoleDiscriminator());
    }

    public static String getOffOnFormatted(StreamlineUser stat){
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat instanceof StreamlineConsole) {
            return GivenConfigs.getMainConfig().userConsoleNameFormatted();
        }

        if (stat instanceof StreamlinePlayer) {
            if (stat.isOnline()) {
                return SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOnlineName());
            } else {
                return SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOfflineName());
            }
        }

        return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
    }

    public static String getOffOnAbsolute(StreamlineUser stat){
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat instanceof StreamlineConsole) {
            return GivenConfigs.getMainConfig().userConsoleNameRegular();
        }

        if (stat instanceof StreamlinePlayer) {
            if (stat.isOnline()) {
                return SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOnlineName());
            } else {
                return SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOfflineName());
            }
        }

        return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
    }

    public static String getFormatted(StreamlineUser stat){
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat instanceof StreamlineConsole) {
            return GivenConfigs.getMainConfig().userConsoleNameFormatted();
        }

        if (stat instanceof StreamlinePlayer) {
            return stat.getDisplayName();
        }

        return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
    }

    public static String getAbsolute(StreamlineUser stat){
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat instanceof StreamlineConsole) {
            return "%";
        }

        if (stat instanceof StreamlinePlayer) {
            return stat.getLatestName();
        }

        return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
    }

    public static String getLuckPermsPrefix(String username){
        User user = SLAPI.getLuckPerms().getUserManager().getUser(username);
        if (user == null) {
            return "";
        }

        String prefix = "";

        Group group = SLAPI.getLuckPerms().getGroupManager().getGroup(user.getPrimaryGroup());
        if (group == null) {
            TreeMap<Integer, String> preWeight = new TreeMap<>();

            for (PrefixNode node : user.getNodes(NodeType.PREFIX)) {
                preWeight.put(node.getPriority(), node.getMetaValue());
            }

            prefix = preWeight.get(MathUtils.getCeilingInt(preWeight.keySet()));

            if (prefix == null) {
                prefix = "";
            }

            return prefix;
        }


        TreeMap<Integer, String> preWeight = new TreeMap<>();

        for (PrefixNode node : group.getNodes(NodeType.PREFIX)) {
            preWeight.put(node.getPriority(), node.getMetaValue());
        }

        for (PrefixNode node : user.getNodes(NodeType.PREFIX)) {
            preWeight.put(node.getPriority(), node.getMetaValue());
        }

        prefix = preWeight.get(MathUtils.getCeilingInt(preWeight.keySet()));

        if (prefix == null) {
            prefix = "";
        }

        return prefix;
    }

    public static String getLuckPermsSuffix(String username){
        User user = SLAPI.getLuckPerms().getUserManager().getUser(username);
        if (user == null) return "";

        String suffix = "";

        Group group = SLAPI.getLuckPerms().getGroupManager().getGroup(user.getPrimaryGroup());
        if (group == null){
            TreeMap<Integer, String> preWeight = new TreeMap<>();

            for (PrefixNode node : user.getNodes(NodeType.PREFIX)) {
                preWeight.put(node.getPriority(), node.getMetaValue());
            }

            suffix = preWeight.get(MathUtils.getCeilingInt(preWeight.keySet()));

            if (suffix == null) {
                suffix = "";
            }

            return suffix;
        }


        TreeMap<Integer, String> sufWeight = new TreeMap<>();

        for (SuffixNode node : group.getNodes(NodeType.SUFFIX)) {
            sufWeight.put(node.getPriority(), node.getMetaValue());
        }

        for (SuffixNode node : user.getNodes(NodeType.SUFFIX)) {
            sufWeight.put(node.getPriority(), node.getMetaValue());
        }

        suffix = sufWeight.get(MathUtils.getCeilingInt(sufWeight.keySet()));

        if (suffix == null) suffix = "";

        return suffix;
    }

    public static String getDisplayName(String username, String nickName) {
        return getLuckPermsPrefix(username) + nickName + getLuckPermsSuffix(username);
    }

    public static StreamlineConsole getConsole() {
        for (StreamlineUser user : getLoadedUsersSet()) {
            if (user instanceof StreamlineConsole console) return console;
        }

        return (StreamlineConsole) loadUser(new StreamlineConsole());
    }

    public static void addPermission(User user, String permission) {
        // Add the permission
        user.data().add(Node.builder(permission).build());

        // Now we need to save changes.
        SLAPI.getLuckPerms().getUserManager().saveUser(user);
    }

    public static void removePermission(User user, String permission) {
        // Add the permission
        user.data().remove(Node.builder(permission).build());

        // Now we need to save changes.
        SLAPI.getLuckPerms().getUserManager().saveUser(user);
    }

    public static boolean runAs(OperatorUser user, String command) {
        return SLAPI.getInstance().getUserManager().runAs(user.getParent(), true, command);
    }

    public static boolean runAs(StreamlineUser user, String command) {
        return SLAPI.getInstance().getUserManager().runAs(user, user.isBypassPermissions(), command);
    }

    public static String getUUIDFromName(String name) {
        return CachedUUIDsHandler.getCachedUUID(name);
    }

    public static StreamlineUser getOrGetUserByName(String name) {
        String uuid = getUUIDFromName(name);
        if (uuid == null) {
//            SLAPI.getInstance().getMessenger().logWarning("Could not get UUID from name '" + name + "'.");
            return null;
        }

        return getOrGetUser(uuid);
    }

    public static StreamlinePlayer getOrGetPlayerByName(String name) {
        String uuid = getUUIDFromName(name);
        if (uuid == null) return null;

        return getOrGetPlayer(uuid);
    }

    public static List<StreamlinePlayer> getPlayersOn(String server) {
        StreamlineServerInfo s = SLAPI.getInstance().getPlatform().getStreamlineServer(server);
        if (s == null) return new ArrayList<>();

        List<StreamlinePlayer> r = new ArrayList<>();

        s.getOnlineUsers().forEach((string, StreamlineUser) -> {
            if (! (StreamlineUser instanceof StreamlinePlayer player)) return;
            r.add(player);
        });

        return r;
    }

    public static boolean isGeyserPlayer(StreamlineUser user) {
        return isGeyserPlayer(user.getUuid());
    }

    public static boolean isGeyserPlayer(String uuid) {
        return SLAPI.getGeyserHolder().isPresent() && SLAPI.getGeyserHolder().isGeyserPlayerByUUID(uuid);
    }
}