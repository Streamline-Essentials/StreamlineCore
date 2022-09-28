package net.streamline.platform.savables;

import lombok.Getter;
import net.luckperms.api.model.user.User;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.interfaces.IUserManager;
import net.streamline.api.messages.builders.ResourcePackMessageBuilder;
import net.streamline.api.messages.builders.ServerConnectMessageBuilder;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.BasePlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class UserManager implements IUserManager {
    @Getter
    private static UserManager instance;

    public UserManager() {
        instance = this;
    }

    public StreamlinePlayer getOrGetPlayer(Player player) {
        return UserUtils.getOrGetPlayer(player.getUniqueId().toString());
    }

    public StreamlineUser getOrGetUser(CommandSender sender) {
        if (isConsole(sender)) {
            return UserUtils.getOrGetUser(GivenConfigs.getMainConfig().userConsoleDiscriminator());
        } else {
            return UserUtils.getOrGetUser(Streamline.getPlayer(sender).getUniqueId().toString());
        }
    }

    public String getUsername(CommandSender sender) {
        if (isConsole(sender)) return GivenConfigs.getMainConfig().userConsoleNameRegular();
        else return sender.getName();
    }

    public String getUsername(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().userConsoleDiscriminator())) return GivenConfigs.getMainConfig().userConsoleNameRegular();
        else {
            Player player = Streamline.getPlayer(uuid);
            if (player == null) return null;
            return getUsername(player);
        }
    }

    public boolean isConsole(CommandSender sender) {
        return sender.equals(Streamline.getInstance().getProxy().getConsoleSender());
    }

    public boolean isOnline(String uuid) {
        if (UserUtils.isConsole(uuid)) return true;
        for (Player player : BasePlugin.onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return true;
        }

        return false;
    }

    public String parsePlayerIP(Player player) {
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        InetSocketAddress address = player.getAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        String ipSt = address.toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    public boolean runAs(StreamlineUser user, boolean bypass, String command) {
        CommandSender source;
        if (user instanceof StreamlinePlayer player) source = Streamline.getPlayer(player.getUuid());
        else {
            source = Streamline.getInstance().getProxy().getConsoleSender();
            Streamline.getInstance().getProxy().dispatchCommand(source, command);
            return true;
        }
        if (source == null) return false;
        boolean already = source.hasPermission("*");
        if (bypass && !already) {
            User u = SLAPI.getLuckPerms().getUserManager().getUser(player.getUuid());
            if (u == null) return false;
            UserUtils.addPermission(u, "*");
        }
        Streamline.getInstance().getProxy().dispatchCommand(source, command);
        if (bypass && !already) {
            User u = SLAPI.getLuckPerms().getUserManager().getUser(player.getUuid());
            if (u == null) return false;
            UserUtils.removePermission(u, "*");
        }
        return true;
    }

    public ConcurrentSkipListSet<StreamlineUser> getUsersOn(String server) {
        ConcurrentSkipListSet<StreamlineUser> r = new ConcurrentSkipListSet<>();

        UserUtils.getLoadedUsersSet().forEach(a -> {
            if (! a.isOnline()) return;
            if (a.getLatestServer().equals(server)) r.add(a);
        });

        return r;
    }

    public void connect(StreamlineUser user, String server) {
        if (! user.isOnline()) return;
        if (user instanceof StreamlineConsole) return;

        Player player = Streamline.getPlayer(user.getUuid());
        if (player == null) return;
        StreamlineServerInfo s = Streamline.getInstance().getStreamlineServer(server);
        SLAPI.getInstance().getProxyMessenger().sendMessage(ServerConnectMessageBuilder.build(s, user));
    }

    public void sendUserResourcePack(StreamlineUser user, StreamlineResourcePack pack) {
        if (! (user instanceof StreamlinePlayer player)) return;
        if (! player.updateOnline()) return;
        Player p = Streamline.getPlayer(user.getUuid());
        if (p == null) return;

        SLAPI.getInstance().getProxyMessenger().sendMessage(ResourcePackMessageBuilder.build(user, pack));
    }

    @Override
    public String parsePlayerIP(String uuid) {
        Player player = Streamline.getPlayer(uuid);
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        InetSocketAddress address = player.getAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        String ipSt = address.toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    @Override
    public double getPlayerPing(String uuid) {
        Player player = Streamline.getPlayer(uuid);
        if (player == null) return 0d;
        return player.getPing();
    }
}
