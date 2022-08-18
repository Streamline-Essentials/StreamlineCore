package net.streamline.platform.savables;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.leonhard.storage.Config;
import de.leonhard.storage.Json;
import de.leonhard.storage.Toml;
import lombok.Getter;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.*;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.interfaces.IUserManager;
import net.streamline.api.messages.ResourcePackMessageBuilder;
import net.streamline.api.messages.ServerConnectMessageBuilder;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.SavableResource;
import net.streamline.api.savables.events.LoadStreamlineUserEvent;
import net.streamline.api.savables.users.OperatorUser;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MathUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.users.SavableConsole;
import net.streamline.platform.users.SavablePlayer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class UserManager implements IUserManager {
    @Getter
    private static UserManager instance;

    public UserManager() {
        instance = this;
    }

    public TreeMap<String, StreamlineUser> loadedUsers = new TreeMap<>();

    public List<StreamlineUser> getLoadedUsers() {
        return new ArrayList<>(loadedUsers.values());
    }

    public StreamlineUser loadUser(StreamlineUser user) {
        loadedUsers.put(user.getUUID(), user);
        user.saveAll();
        ModuleUtils.fireEvent(new LoadStreamlineUserEvent<>(user));
        return user;
    }

    public void unloadUser(StreamlineUser user) {
        loadedUsers.remove(user.getUUID());
    }

    private StreamlineUser getUser(String uuid) {
        return loadedUsers.get(uuid);
    }

    private StreamlinePlayer getPlayer(Player player) {
        return (StreamlinePlayer) loadedUsers.get(player.getUniqueId().toString());
    }

    public boolean userExists(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().userConsoleDiscriminator())) return getLoadedUsers().contains(getConsole());
        StorageUtils.StorageType type = GivenConfigs.getMainConfig().userUseType();
        File userFolder = Streamline.getInstance().getUserFolder();
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
                        StorageUtils.getWhere("whitelistedUuid", uuid)
                );
            }
            case MYSQL -> {
                return GivenConfigs.getMainConfig().getConfiguredDatabase().mySQLConnection().exists(
                        new SQLCollection(StreamlinePlayer.class.getSimpleName(),
                                "whitelistedUuid",
                                uuid
                        )
                );
            }
            default -> {
                return false;
            }
        }
    }

    public StreamlineUser getOrGetUser(String uuid) {
        StreamlineUser user = getUser(uuid);
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
    public StreamlineUser getOrGetOrGetUser(String uuid) {
        StreamlineUser user = getOrGetUser(uuid);
        if (user != null) return user;

        if (isConsole(uuid)) {
            user = new SavableConsole();
        } else {
            user = new SavablePlayer(uuid);
        }

        loadUser(user);
        return user;
    }

    public StreamlinePlayer getOrGetPlayer(Player player) {
        StreamlinePlayer user = getPlayer(player);
        if (user != null) return user;

        user = new SavablePlayer(player.getUniqueId());

        loadUser(user);
        return user;
    }

    public StreamlinePlayer getOrGetPlayer(String uuid) {
        StreamlineUser user = getOrGetUser(uuid);
        if (! (user instanceof StreamlinePlayer StreamlinePlayer)) return null;

        return StreamlinePlayer;
    }

    public StreamlineUser getOrGetUser(CommandSource sender) {
        if (isConsole(sender)) {
            return getOrGetUser(GivenConfigs.getMainConfig().userConsoleDiscriminator());
        } else {
            return getOrGetUser(Streamline.getPlayer(sender).getUniqueId().toString());
        }
    }

    public StorageResource<?> newStorageResource(String uuid, Class<? extends SavableResource> clazz) {
        switch (GivenConfigs.getMainConfig().userUseType()) {
            case YAML -> {
                return new FlatFileResource<>(Config.class, uuid + ".yml", Streamline.getInstance().getUserFolder(), false);
            }
            case JSON -> {
                return new FlatFileResource<>(Json.class, uuid + ".json", Streamline.getInstance().getUserFolder(), false);
            }
            case TOML -> {
                return new FlatFileResource<>(Toml.class, uuid + ".toml", Streamline.getInstance().getUserFolder(), false);
            }
            case MONGO -> {
                return new MongoResource(GivenConfigs.getMainConfig().getConfiguredDatabase(), clazz.getSimpleName(), "whitelistedUuid", uuid);
            }
            case MYSQL -> {
                return new MySQLResource(GivenConfigs.getMainConfig().getConfiguredDatabase(), new SQLCollection(clazz.getSimpleName(), "whitelistedUuid", uuid));
            }
        }

        return null;
    }

    public String getUsername(CommandSource sender) {
        if (isConsole(sender)) return GivenConfigs.getMainConfig().userConsoleNameRegular();
        else return ((Player) sender).getUsername();
    }

    public String getUsername(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().userConsoleDiscriminator())) return GivenConfigs.getMainConfig().userConsoleNameRegular();
        else {
            Player player = Streamline.getPlayer(uuid);
            if (player == null) return null;
            return getUsername(player);
        }
    }

    public boolean isConsole(CommandSource sender) {
        return sender.equals(Streamline.getInstance().getProxy().getConsoleCommandSource());
    }

    public boolean isConsole(String uuid) {
        return uuid.equals(GivenConfigs.getMainConfig().userConsoleDiscriminator());
    }

    public boolean isOnline(String uuid) {
        if (isConsole(uuid)) return true;
        for (Player player : BasePlugin.onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return true;
        }

        return false;
    }

    public String parsePlayerIP(Player player) {
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        InetSocketAddress address = player.getRemoteAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        String ipSt = address.toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    public String getOffOnFormatted(StreamlineUser stat){
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat instanceof StreamlineConsole) {
            return GivenConfigs.getMainConfig().userConsoleNameFormatted();
        }

        if (stat instanceof StreamlinePlayer) {
            if (stat.isOnline()) {
                return Streamline.getInstance().getMessenger().replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOnlineName());
            } else {
                return Streamline.getInstance().getMessenger().replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOfflineName());
            }
        }

        return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
    }

    public String getOffOnAbsolute(StreamlineUser stat){
        if (stat == null) {
            return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        }

        if (stat instanceof StreamlineConsole) {
            return GivenConfigs.getMainConfig().userConsoleNameRegular();
        }

        if (stat instanceof StreamlinePlayer) {
            if (stat.isOnline()) {
                return Streamline.getInstance().getMessenger().replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOnlineName());
            } else {
                return Streamline.getInstance().getMessenger().replaceAllPlayerBungee(stat, GivenConfigs.getMainConfig().playerOfflineName());
            }
        }

        return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
    }

    public String getFormatted(StreamlineUser stat){
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

    public String getAbsolute(StreamlineUser stat){
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

    public String getLuckPermsPrefix(String username){
        User user = Streamline.getInstance().getLuckPerms().getUserManager().getUser(username);
        if (user == null) {
            return "";
        }

        String prefix = "";

        Group group = Streamline.getInstance().getLuckPerms().getGroupManager().getGroup(user.getPrimaryGroup());
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

    public String getLuckPermsSuffix(String username){
        User user = Streamline.getInstance().getLuckPerms().getUserManager().getUser(username);
        if (user == null) return "";

        String suffix = "";

        Group group = Streamline.getInstance().getLuckPerms().getGroupManager().getGroup(user.getPrimaryGroup());
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

    public String getDisplayName(String username, String nickName) {
        return getLuckPermsPrefix(username) + nickName + getLuckPermsSuffix(username);
    }

    public StreamlineConsole getConsole() {
        for (StreamlineUser user : getLoadedUsers()) {
            if (user instanceof StreamlineConsole console) return console;
        }

        return (StreamlineConsole) loadUser(new SavableConsole());
    }

    public void addPermission(User user, String permission) {
        // Add the permission
        user.data().add(Node.builder(permission).build());

        // Now we need to save changes.
        Streamline.getInstance().getLuckPerms().getUserManager().saveUser(user);
    }

    public void removePermission(User user, String permission) {
        // Add the permission
        user.data().remove(Node.builder(permission).build());

        // Now we need to save changes.
        Streamline.getInstance().getLuckPerms().getUserManager().saveUser(user);
    }

    public boolean runAs(OperatorUser user, String command) {
        return runAs(user.getParent(), true, command);
    }

    public boolean runAs(StreamlineUser user, String command) {
        return runAs(user, user.isBypassPermissions(), command);
    }

    public boolean runAs(StreamlineUser user, boolean bypass, String command) {
        CommandSource source;
        if (user instanceof StreamlinePlayer player) source = Streamline.getPlayer(player.getUUID());
        else {
            source = Streamline.getInstance().getProxy().getConsoleCommandSource();
            Streamline.getInstance().getProxy().getCommandManager().executeAsync(source, command);
            return true;
        }
        if (source == null) return false;
        boolean already = source.hasPermission("*");
        if (bypass && !already) {
            User u = Streamline.getInstance().getLuckPerms().getUserManager().getUser(player.getUUID());
            if (u == null) return false;
            addPermission(u, "*");
        }
        Streamline.getInstance().getProxy().getCommandManager().executeAsync(source, command);
        if (bypass && !already) {
            User u = Streamline.getInstance().getLuckPerms().getUserManager().getUser(player.getUUID());
            if (u == null) return false;
            removePermission(u, "*");
        }
        return true;
    }

    public StreamlineUser getOrGetUserByName(String name) {
        String uuid = Streamline.getInstance().getUUIDFromName(name);
        if (uuid == null) {
            if (name.equals(GivenConfigs.getMainConfig().userConsoleNameRegular())) return getConsole();
            return null;
        }

        return getOrGetUser(uuid);
    }

    public List<StreamlinePlayer> getPlayersOn(String server) {
        StreamlineServerInfo s = Streamline.getInstance().getStreamlineServer(server);
        if (s == null) return new ArrayList<>();

        List<StreamlinePlayer> r = new ArrayList<>();

        s.getOnlineUsers().forEach((string, StreamlineUser) -> {
            if (! (StreamlineUser instanceof StreamlinePlayer player)) return;
            r.add(player);
        });

        return r;
    }

    public List<StreamlineUser> getUsersOn(String server) {
        List<StreamlineUser> r = new ArrayList<>();

        Streamline.getInstance().getProxy().getAllServers().forEach(a -> {
            a.getPlayersConnected().forEach(b -> {
                r.add(getOrGetUser(b));
            });
        });

        return r;
    }

    public void connect(StreamlineUser user, String server) {
        if (! user.isOnline()) return;
        if (user instanceof StreamlineConsole) return;

        Player player = Streamline.getPlayer(user.getUUID());
        if (player == null) return;
        StreamlineServerInfo s = Streamline.getInstance().getStreamlineServer(server);
        SLAPI.getInstance().getProxyMessenger().sendMessage(ServerConnectMessageBuilder.build(s, user));
    }

    public boolean isGeyserPlayer(StreamlineUser user) {
        return isGeyserPlayer(user.getUUID());
    }

    public boolean isGeyserPlayer(String uuid) {
        return Streamline.getInstance().getGeyserHolder().isPresent() && Streamline.getInstance().getGeyserHolder().isGeyserPlayerByUUID(uuid);
    }

    public void sendUserResourcePack(StreamlineUser user, StreamlineResourcePack pack) {
        if (! (user instanceof StreamlinePlayer player)) return;
        if (! player.updateOnline()) return;
        Player p = Streamline.getPlayer(user.getUUID());
        if (p == null) return;

        SLAPI.getInstance().getProxyMessenger().sendMessage(ResourcePackMessageBuilder.build(user, pack));
    }
}
