package net.streamline.api.utils;

import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.CachedUUIDsHandler;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.MongoUserResource;
import net.streamline.api.savables.SQLUserResource;
import net.streamline.api.savables.events.LoadStreamlineUserEvent;
import net.streamline.api.savables.users.*;
import org.jetbrains.annotations.NotNull;
import tv.quaint.storage.StorageUtils;
import tv.quaint.storage.resources.StorageResource;
import tv.quaint.storage.resources.databases.configurations.DatabaseConfig;
import tv.quaint.storage.resources.flat.FlatFileResource;
import tv.quaint.thebase.lib.leonhard.storage.Config;
import tv.quaint.thebase.lib.leonhard.storage.Json;
import tv.quaint.thebase.lib.leonhard.storage.Toml;

import java.io.File;
import java.sql.PreparedStatement;
import java.util.TreeMap;
import java.util.UUID;
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

    public static ConcurrentSkipListMap<String, StreamlinePlayer> getLoadedPlayers() {
        ConcurrentSkipListMap<String, StreamlinePlayer> r = new ConcurrentSkipListMap<>();

        getLoadedUsers().forEach((s, user) -> {
            if (user instanceof StreamlinePlayer) r.put(user.getUuid(), (StreamlinePlayer) user);
        });

        return r;
    }

    public static StreamlineUser loadUser(StreamlineUser user) {
        getLoadedUsers().put(user.getUuid(), user);
        ModuleUtils.fireEvent(new LoadStreamlineUserEvent<>(user));
        return user;
    }

    public static void unloadUser(StreamlineUser user) {
        unloadUser(user.getUuid());
    }

    public static void unloadUser(String uuid) {
        if (! isLoaded(uuid)) return;
        StreamlineUser user = getUser(uuid);
        user.saveAll();
        getLoadedUsers().remove(uuid);
    }

    public static boolean isLoaded(String uuid) {
        return getUser(uuid) != null;
    }

    public static ConcurrentSkipListMap<String, StreamlineUser> getOnlineUsers() {
        ConcurrentSkipListMap<String, StreamlineUser> r = new ConcurrentSkipListMap<>();

        getLoadedUsers().forEach((s, user) -> {
            if (user.updateOnline()) r.put(user.getUuid(), user);
        });

        return r;
    }

    public static ConcurrentSkipListMap<String, StreamlinePlayer> getOnlinePlayers() {
        ConcurrentSkipListMap<String, StreamlinePlayer> r = new ConcurrentSkipListMap<>();

        getOnlineUsers().forEach((s, user) -> {
            if (user instanceof StreamlinePlayer) r.put(user.getUuid(), (StreamlinePlayer) user);
        });

        return r;
    }

    private static StreamlineUser getUser(String uuid) {
        return getLoadedUsers().get(uuid);
    }

    public static boolean userExists(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().userConsoleDiscriminator())) return getLoadedUsersSet().contains(getConsole());
        StorageUtils.SupportedStorageType type = GivenConfigs.getMainConfig().userUseType();
        DatabaseConfig config = GivenConfigs.getMainConfig().getConfiguredDatabase();
        File userFolder = SLAPI.getUserFolder();
        switch (type) {
            case YAML:
                File[] files = userFolder.listFiles();
                if (files == null) return false;

                for (File file : files) {
                    if (file.getName().equals(uuid + ".yml")) return true;
                }
                return false;
            case JSON:
                File[] files2 = userFolder.listFiles();
                if (files2 == null) return false;

                for (File file : files2) {
                    if (file.getName().equals(uuid + ".json")) return true;
                }
                return false;
            case TOML:
                File[] files3 = userFolder.listFiles();
                if (files3 == null) return false;

                for (File file : files3) {
                    if (file.getName().equals(uuid + ".toml")) return true;
                }
                return false;
            case MONGO:
                if (uuid.equals(GivenConfigs.getMainConfig().userConsoleDiscriminator())) {
                    MongoUserResource<StreamlineConsole> resource = new MongoUserResource<>(uuid, config, StreamlineConsole.class);
                    return resource.exists();
                } else {
                    MongoUserResource<StreamlinePlayer> resource = new MongoUserResource<>(uuid, config, StreamlinePlayer.class);
                    return resource.exists();
                }
            case MYSQL:
            case SQLITE:
                if (uuid.equals(GivenConfigs.getMainConfig().userConsoleDiscriminator())) {
                    SQLUserResource<StreamlineConsole> resource = new SQLUserResource<>(uuid, config, StreamlineConsole.class);
                    return resource.exists();
                } else {
                    SQLUserResource<StreamlinePlayer> resource = new SQLUserResource<>(uuid, config, StreamlinePlayer.class);
                    return resource.exists();
                }
            default:
                return false;
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
        if (! (user instanceof StreamlinePlayer)) return null;
        return (StreamlinePlayer) user;
    }

    public static <T extends StreamlineUser> String getTableNameByUserType(Class<T> userClass) {
        if (userClass == StreamlinePlayer.class) return "players";
        if (userClass == StreamlineConsole.class) return "consoles";
        return "users";
    }

    public static <T extends StreamlineUser> void ensureTable(T userClass, SQLUserResource<T> resource) {
        if (userClass.getClass() == StreamlinePlayer.class) {
            try {
                PreparedStatement statement = resource.getProvider().getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + resource.getTable() +
                        " (uuid VARCHAR(36) PRIMARY KEY, " +
                        "latestName VARCHAR(128)" +
                        "displayName VARCHAR(128)" +
                        "tags VARCHAR(4096)" +
                        "points DOUBLE" +
                        "lastMessage VARCHAR(4096)" +
                        "latestServer VARCHAR(128)" +
                        "totalXP DOUBLE" +
                        "currentXP DOUBLE" +
                        "level INT" +
                        "playSeconds INT" +
                        "latestIP VARCHAR(128)" +
                        "ips VARCHAR(4096)" +
                        "names VARCHAR(4096)" +
                        ");");
                statement.execute();
                statement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                PreparedStatement statement = resource.getProvider().getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS " + resource.getTable() +
                        " (uuid VARCHAR(36) PRIMARY KEY, " +
                        "latestName VARCHAR(128)" +
                        "displayName VARCHAR(128)" +
                        "tags VARCHAR(4096)" +
                        "points DOUBLE" +
                        "lastMessage VARCHAR(4096)" +
                        "latestServer VARCHAR(128)" +
                        ");");
                statement.execute();
                statement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static <T extends StreamlineUser> StorageResource<?> newUserStorageResource(String uuid, Class<T> user) {
        switch (GivenConfigs.getMainConfig().userUseType()) {
            case YAML:
                return new FlatFileResource<>(Config.class, uuid + ".yml", SLAPI.getUserFolder(), false);
            case JSON:
                return new FlatFileResource<>(Json.class, uuid + ".json", SLAPI.getUserFolder(), false);
            case TOML:
                return new FlatFileResource<>(Toml.class, uuid + ".toml", SLAPI.getUserFolder(), false);
            case MONGO:
                return new MongoUserResource<>(uuid, GivenConfigs.getMainConfig().getConfiguredDatabase(), user);
            case MYSQL:
            case SQLITE:
                return new SQLUserResource<>(uuid, GivenConfigs.getMainConfig().getConfiguredDatabase(), user);
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
                return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOnlineName());
            } else {
                return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOfflineName());
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
                return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOnlineName());
            } else {
                return MessageUtils.replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOfflineName());
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
        User user;
        if (username.contains("-")) user = SLAPI.getLuckPerms().getUserManager().getUser(UUID.fromString(username));
        else user = SLAPI.getLuckPerms().getUserManager().getUser(username);
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
        User user;
        if (username.contains("-")) user = SLAPI.getLuckPerms().getUserManager().getUser(UUID.fromString(username));
        else user = SLAPI.getLuckPerms().getUserManager().getUser(username);
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

    public static String getFormattedDefaultNickname(StreamlineUser user) {
        String defaultNick = GivenConfigs.getMainConfig().userCombinedNicknameDefault();

        return ModuleUtils.replacePlaceholders(user, defaultNick);
    }

    public static StreamlineConsole getConsole() {
        for (StreamlineUser user : getLoadedUsersSet()) {
            if (user instanceof StreamlineConsole) return (StreamlineConsole) user;
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
//            MessageUtils.logWarning("Could not get UUID from name '" + name + "'.");
            return null;
        }

        return getOrGetUser(uuid);
    }

    public static StreamlinePlayer getOrGetPlayerByName(String name) {
        String uuid = getUUIDFromName(name);
        if (uuid == null) return null;

        return getOrGetPlayer(uuid);
    }

    public static ConcurrentSkipListSet<StreamlinePlayer> getPlayersOn(String server) {
        StreamlineServerInfo s = GivenConfigs.getProfileConfig().getServerInfo(server);
        if (s == null) return new ConcurrentSkipListSet<>();

        ConcurrentSkipListSet<StreamlinePlayer> r = new ConcurrentSkipListSet<>();

        s.getOnlineUsers().forEach((string) -> {
            StreamlinePlayer player = getOrGetPlayer(string);
            if (player == null) return;
            r.add(player);
        });

        return r;
    }

    public static ConcurrentSkipListSet<StreamlineUser> getUsersOn(String server) {
        StreamlineServerInfo s = GivenConfigs.getProfileConfig().getServerInfo(server);
        if (s == null) return new ConcurrentSkipListSet<>();

        ConcurrentSkipListSet<StreamlineUser> r = new ConcurrentSkipListSet<>();

        s.getOnlineUsers().forEach((string) -> {
            StreamlineUser player = getOrGetUser(string);
            if (player == null) return;
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
