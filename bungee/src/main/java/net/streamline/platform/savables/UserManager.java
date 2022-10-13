package net.streamline.platform.savables;

import lombok.Getter;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
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
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;

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

    public StreamlinePlayer getOrGetPlayer(ProxiedPlayer player) {
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

    @Override
    public String getUsername(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().userConsoleDiscriminator())) return GivenConfigs.getMainConfig().userConsoleNameRegular();
        else {
            ProxiedPlayer player = Streamline.getPlayer(uuid);
            if (player == null) return null;
            return getUsername(player);
        }
    }

    public boolean isConsole(CommandSender sender) {
        return sender.equals(Streamline.getInstance().getProxy().getConsole());
    }

    @Override
    public boolean isOnline(String uuid) {
        if (UserUtils.isConsole(uuid)) return true;
        for (ProxiedPlayer player : BasePlugin.onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return true;
        }

        return false;
    }

    public String parsePlayerIP(ProxiedPlayer player) {
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        InetSocketAddress address = player.getAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        String ipSt = address.toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    @Override
    public boolean runAs(StreamlineUser user, boolean bypass, String command) {
        CommandSender source;
        if (user instanceof StreamlinePlayer player) source = Streamline.getPlayer(player.getUuid());
        else {
            source = Streamline.getInstance().getProxy().getConsole();
            Streamline.getInstance().getProxy().getPluginManager().dispatchCommand(source, command);
            return true;
        }
        if (source == null) return false;
        boolean already = source.hasPermission("*");
        if (bypass && !already) {
            User u = SLAPI.getLuckPerms().getUserManager().getUser(player.getUuid());
            if (u == null) return false;
            UserUtils.addPermission(u, "*");
        }
        Streamline.getInstance().getProxy().getPluginManager().dispatchCommand(source, command);
        if (bypass && !already) {
            User u = SLAPI.getLuckPerms().getUserManager().getUser(player.getUuid());
            if (u == null) return false;
            UserUtils.removePermission(u, "*");
        }
        return true;
    }

    @Override
    public ConcurrentSkipListSet<StreamlineUser> getUsersOn(String server) {
        ConcurrentSkipListSet<StreamlineUser> r = new ConcurrentSkipListSet<>();

        Streamline.getInstance().getProxy().getServers().values().forEach(a -> {
            a.getPlayers().forEach(b -> {
                r.add(getOrGetUser(b));
            });
        });

        return r;
    }

    @Override
    public void connect(StreamlineUser user, String server) {
        if (! user.isOnline()) return;
        if (user instanceof StreamlineConsole) return;

        ProxiedPlayer player = Streamline.getPlayer(user.getUuid());
        if (player == null) return;
        ServerInfo serverInfo = Streamline.getInstance().getProxy().getServerInfo(server);

        if (serverInfo == null) {
            MessageUtils.logWarning("Tried to send a user with uuid of '" + user.getUuid() + "' to server '" + server + "', but it does not exist!");
            return;
        }

        player.connect(serverInfo);
    }

    @Override
    public void sendUserResourcePack(StreamlineUser user, StreamlineResourcePack pack) {
        if (! (user instanceof StreamlinePlayer player)) return;
        if (! player.updateOnline()) return;
        ProxiedPlayer p = Streamline.getPlayer(user.getUuid());
        if (p == null) return;
        StreamlinePlayer pl = getOrGetPlayer(p);

        SLAPI.getInstance().getProxyMessenger().sendMessage(ResourcePackMessageBuilder.build(pl, true, pl, pack));
    }

    @Override
    public String parsePlayerIP(String uuid) {
        ProxiedPlayer player = Streamline.getPlayer(uuid);
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        InetSocketAddress address = (InetSocketAddress) player.getSocketAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        String ipSt = address.toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    @Override
    public double getPlayerPing(String uuid) {
        ProxiedPlayer player = Streamline.getPlayer(uuid);
        if (player == null) return 0d;
        return player.getPing();
    }

    @Override
    public void kick(StreamlineUser user, String message) {
        ProxiedPlayer player = Streamline.getInstance().getProxy().getPlayer(user.getUuid());
        if (player == null) return;
        player.disconnect(Messenger.getInstance().codedText(message));
    }
}
