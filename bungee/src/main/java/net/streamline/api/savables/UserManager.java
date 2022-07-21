package net.streamline.api.savables;

import de.leonhard.storage.Config;
import de.leonhard.storage.Json;
import de.leonhard.storage.Toml;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.configs.*;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.events.LoadSavableUserEvent;
import net.streamline.base.Streamline;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.api.savables.users.SavableConsole;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.utils.MathUtils;
import net.streamline.utils.MessagingUtils;
import net.streamline.utils.UUIDUtils;

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
        ModuleUtils.fireEvent(new LoadSavableUserEvent<>(user));
        return user;
    }

    public static void unloadUser(SavableUser user) {
        loadedUsers.remove(user.uuid);
    }

    private static SavableUser getUser(String uuid) {
        return loadedUsers.get(uuid);
    }

    private static SavablePlayer getPlayer(ProxiedPlayer player) {
        return (SavablePlayer) loadedUsers.get(player.getUniqueId().toString());
    }

    public static SavableUser getOrGetUser(String uuid) {
        SavableUser user = getUser(uuid);
        if (user != null) return user;

        if (isConsole(uuid)) {
            user = new SavableConsole();
        } else {
            user = new SavablePlayer(uuid);
        }

        loadUser(user);
        return user;
    }

    public static SavablePlayer getOrGetPlayer(ProxiedPlayer player) {
        SavablePlayer user = getPlayer(player);
        if (user != null) return user;

        user = new SavablePlayer(player);

        loadUser(user);
        return user;
    }

    public static SavablePlayer getOrGetPlayer(String uuid) {
        SavableUser user = getOrGetUser(uuid);
        if (! (user instanceof SavablePlayer savablePlayer)) return null;

        return savablePlayer;
    }

    public static SavableUser getOrGetUser(CommandSender sender) {
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

    public static String getUsername(CommandSender sender) {
        if (isConsole(sender)) return Streamline.getMainConfig().userConsoleNameRegular();
        else return sender.getName();
    }

    public static String getUsername(String uuid) {
        if (uuid.equals(Streamline.getMainConfig().userConsoleDiscriminator())) return Streamline.getMainConfig().userConsoleNameRegular();
        else {
            ProxiedPlayer player = Streamline.getPlayer(uuid);
            if (player == null) return null;
            return getUsername(player);
        }
    }

    public static boolean isConsole(CommandSender sender) {
        return sender.equals(Streamline.getInstance().getProxy().getConsole());
    }

    public static boolean isConsole(String uuid) {
        return uuid.equals(Streamline.getMainConfig().userConsoleDiscriminator());
    }

    public static boolean isOnline(String uuid) {
        for (ProxiedPlayer player : Streamline.getInstance().onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return true;
        }

        return false;
    }

    public static String parsePlayerIP(ProxiedPlayer player) {
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        String ipSt = player.getSocketAddress().toString().replace("/", "");
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
}
