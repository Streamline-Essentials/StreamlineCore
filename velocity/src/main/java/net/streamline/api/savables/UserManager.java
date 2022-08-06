package net.streamline.api.savables;

import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.leonhard.storage.Config;
import de.leonhard.storage.Json;
import de.leonhard.storage.Toml;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.streamline.api.BasePlugin;
import net.streamline.api.configs.*;
import net.streamline.api.savables.events.LoadSavableUserEvent;
import net.streamline.api.savables.users.OperatorUser;
import net.streamline.base.Streamline;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.api.savables.users.SavableConsole;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.utils.MathUtils;
import net.streamline.utils.MessagingUtils;
import net.streamline.utils.UUIDUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class UserManager {
    public static TreeMap<String, SavableUser> loadedUsers = new TreeMap<>();

    public static List<SavableUser> getLoadedUsers() {
        return new ArrayList<>(loadedUsers.values());
    }

    public static SavableUser loadUser(SavableUser user) {
        loadedUsers.put(user.uuid, user);
        user.saveAll();
        Streamline.fireEvent(new LoadSavableUserEvent<>(user));
        return user;
    }

    public static void unloadUser(SavableUser user) {
        loadedUsers.remove(user.uuid);
    }

    private static SavableUser getUser(String uuid) {
        return loadedUsers.get(uuid);
    }

    private static SavablePlayer getPlayer(Player player) {
        return (SavablePlayer) loadedUsers.get(player.getUniqueId().toString());
    }

    public static String getSourceName(CommandSource source){
        if (source == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        if (! (source instanceof ConsoleCommandSource)) return ((Player) source).getUsername();
        else return Streamline.getMainConfig().userConsoleNameRegular();
    }

    public static boolean userExists(String uuid) {
        if (uuid.equals(Streamline.getMainConfig().userConsoleDiscriminator())) return getLoadedUsers().contains(getConsole());
        StorageUtils.StorageType type = Streamline.getMainConfig().userUseType();
        File userFolder = Streamline.getUserFolder();
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
                return Streamline.getMainConfig().getConfiguredDatabase().mongoConnection().exists(
                        SavablePlayer.class.getSimpleName(),
                        StorageUtils.getWhere("uuid", uuid)
                );
            }
            case MYSQL -> {
                return Streamline.getMainConfig().getConfiguredDatabase().mySQLConnection().exists(
                        new SQLCollection(SavablePlayer.class.getSimpleName(),
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

    public static SavableUser getOrGetUser(String uuid) {
        SavableUser user = getUser(uuid);
        if (user != null) return user;

        if (isConsole(uuid)) {
            user = new SavableConsole();
        } else {
            if (! userExists(uuid)) return null;
            user = new SavablePlayer(uuid);
        }

        loadUser(user);
        return user;
    }

    @NotNull
    public static SavableUser getOrGetOrGetUser(String uuid) {
        SavableUser user = getOrGetUser(uuid);
        if (user != null) return user;

        if (isConsole(uuid)) {
            user = new SavableConsole();
        } else {
            user = new SavablePlayer(uuid);
        }

        loadUser(user);
        return user;
    }

    public static SavablePlayer getOrGetPlayer(Player player) {
        SavablePlayer user = getPlayer(player);
        if (user != null) return user;

        user = new SavablePlayer(player.getUniqueId().toString());

        loadUser(user);
        return user;
    }

    public static SavablePlayer getOrGetPlayer(String uuid) {
        SavableUser user = getOrGetUser(uuid);
        if (! (user instanceof SavablePlayer savablePlayer)) return null;

        return savablePlayer;
    }

    public static SavableUser getOrGetUser(CommandSource sender) {
        if (isConsole(sender)) {
            return getOrGetUser(Streamline.getMainConfig().userConsoleDiscriminator());
        } else {
            return getOrGetUser(Streamline.getInstance().getPlayer(sender).getUniqueId().toString());
        }
    }

    public static StorageResource<?> newStorageResource(String uuid, Class<? extends SavableResource> clazz) {
        switch (Streamline.getMainConfig().userUseType()) {
            case YAML -> {
                return new FlatFileResource<>(Config.class, uuid + ".yml", Streamline.getUserFolder(), false);
            }
            case JSON -> {
                return new FlatFileResource<>(Json.class, uuid + ".json", Streamline.getUserFolder(), false);
            }
            case TOML -> {
                return new FlatFileResource<>(Toml.class, uuid + ".toml", Streamline.getUserFolder(), false);
            }
            case MONGO -> {
                return new MongoResource(Streamline.getMainConfig().getConfiguredDatabase(), clazz.getSimpleName(), "uuid", uuid);
            }
            case MYSQL -> {
                return new MySQLResource(Streamline.getMainConfig().getConfiguredDatabase(), new SQLCollection(clazz.getSimpleName(), "uuid", uuid));
            }
        }

        return null;
    }

    public static String getUsername(CommandSource sender) {
        if (isConsole(sender)) return Streamline.getMainConfig().userConsoleNameRegular();
        else return ((Player) sender).getUsername();
    }

    public static String getUsername(String uuid) {
        if (uuid.equals(Streamline.getMainConfig().userConsoleDiscriminator())) return Streamline.getMainConfig().userConsoleNameRegular();
        else {
            Player player = Streamline.getPlayer(uuid);
            if (player == null) return null;
            return getUsername(player);
        }
    }

    public static boolean isConsole(CommandSource sender) {
        return sender.equals(Streamline.getInstance().getProxy().getConsoleCommandSource());
    }

    public static boolean isConsole(String uuid) {
        return uuid.equals(Streamline.getMainConfig().userConsoleDiscriminator());
    }

    public static boolean isOnline(String uuid) {
        if (isConsole(uuid)) return true;
        for (Player player : BasePlugin.onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return true;
        }

        return false;
    }

    public static String parsePlayerIP(Player player) {
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        String ipSt = player.getRemoteAddress().toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    public static String getOffOnFormatted(SavableUser stat){
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat instanceof SavableConsole) {
            return Streamline.getMainConfig().userConsoleNameFormatted();
        }

        if (stat instanceof SavablePlayer) {
            if (stat.online) {
                return MessagingUtils.replaceAllPlayerBungee(stat, Streamline.getMainConfig().playerOnlineName());
            } else {
                return MessagingUtils.replaceAllPlayerBungee(stat, Streamline.getMainConfig().playerOfflineName());
            }
        }

        return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
    }

    public static String getOffOnAbsolute(SavableUser stat){
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat instanceof SavableConsole) {
            return Streamline.getMainConfig().userConsoleNameRegular();
        }

        if (stat instanceof SavablePlayer) {
            if (stat.online) {
                return MessagingUtils.replaceAllPlayerBungee(stat, Streamline.getMainConfig().playerOnlineName());
            } else {
                return MessagingUtils.replaceAllPlayerBungee(stat, Streamline.getMainConfig().playerOfflineName());
            }
        }

        return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
    }

    public static String getFormatted(SavableUser stat){
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat instanceof SavableConsole) {
            return Streamline.getMainConfig().userConsoleNameFormatted();
        }

        if (stat instanceof SavablePlayer) {
            return stat.displayName;
        }

        return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
    }

    public static String getAbsolute(SavableUser stat){
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat instanceof SavableConsole) {
            return "%";
        }

        if (stat instanceof SavablePlayer) {
            return stat.latestName;
        }

        return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
    }

    public static String getLuckPermsPrefix(String username){
        User user = Streamline.getLuckPerms().getUserManager().getUser(username);
        if (user == null) {
            return "";
        }

        String prefix = "";

        Group group = Streamline.getLuckPerms().getGroupManager().getGroup(user.getPrimaryGroup());
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
        User user = Streamline.getLuckPerms().getUserManager().getUser(username);
        if (user == null) return "";

        String suffix = "";

        Group group = Streamline.getLuckPerms().getGroupManager().getGroup(user.getPrimaryGroup());
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

    public static SavableConsole getConsole() {
        for (SavableUser user : getLoadedUsers()) {
            if (user instanceof SavableConsole console) return console;
        }

        return (SavableConsole) loadUser(new SavableConsole());
    }

    public static void addPermission(User user, String permission) {
        // Add the permission
        user.data().add(Node.builder(permission).build());

        // Now we need to save changes.
        Streamline.getLuckPerms().getUserManager().saveUser(user);
    }

    public static void removePermission(User user, String permission) {
        // Add the permission
        user.data().remove(Node.builder(permission).build());

        // Now we need to save changes.
        Streamline.getLuckPerms().getUserManager().saveUser(user);
    }

    public static boolean runAs(OperatorUser user, String command) {
        return runAs(user.getParent(), true, command);
    }

    public static boolean runAs(SavableUser user, String command) {
        return runAs(user, user.isBypassPermissions(), command);
    }

    public static boolean runAs(SavableUser user, boolean bypass, String command) {
        CommandSource source;
        if (user instanceof SavablePlayer player) source = Streamline.getPlayer(player.uuid);
        else {
            source = Streamline.getInstance().getProxy().getConsoleCommandSource();
            Streamline.getInstance().getProxy().getCommandManager().executeAsync(source, command);
            return true;
        }
        if (source == null) return false;
        boolean already = source.hasPermission("*");
        if (bypass && !already) {
            User u = Streamline.getLuckPerms().getUserManager().getUser(player.uuid);
            if (u == null) return false;
            addPermission(u, "*");
        }
        Streamline.getInstance().getProxy().getCommandManager().executeAsync(source, command);
        if (bypass && !already) {
            User u = Streamline.getLuckPerms().getUserManager().getUser(player.uuid);
            if (u == null) return false;
            removePermission(u, "*");
        }
        return true;
    }

    public static SavableUser getOrGetUserByName(String name) {
        String uuid = Streamline.getUUIDFromName(name);
        if (uuid == null) {
            if (name.equals(Streamline.getMainConfig().userConsoleNameRegular())) return getConsole();
            return null;
        }

        return getOrGetUser(uuid);
    }

    public static List<Player> getPlayersOn(String server) {
        RegisteredServer s = Streamline.getServer(server);
        if (s == null) return new ArrayList<>();

        return new ArrayList<>(s.getPlayersConnected());
    }

    public static List<SavableUser> getUsersOn(String server) {
        List<SavableUser> r = new ArrayList<>();

        getPlayersOn(server).forEach(a -> {
            SavablePlayer player = getOrGetPlayer(a);
            player.setLatestServer(server);
            r.add(player);
        });

        return r;
    }

    public static void connect(SavableUser user, String server) {
        if (! user.online) return;
        if (user instanceof SavableConsole) return;

        Player player = Streamline.getPlayer(user.uuid);
        if (player == null) return;
        RegisteredServer s = Streamline.getServer(server);
        if (s == null) {
            MessagingUtils.logWarning("Could not send user with name of '" + user.latestName + "' to server '" + server + "' because that server is 'null'");
            return;
        }
        player.createConnectionRequest(s).connect();
    }
}
