package net.streamline.platform.savables;

import lombok.Getter;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.interfaces.IUserManager;
import net.streamline.api.messages.builders.ResourcePackMessageBuilder;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class UserManager implements IUserManager<ProxiedPlayer> {
    @Getter
    private static UserManager instance;

    public UserManager() {
        instance = this;
    }

    public StreamPlayer getOrGetPlayer(ProxiedPlayer player) {
        StreamPlayer p = UserUtils.getOrGetPlayer(player.getUniqueId().toString()).orElse(null);
        if (p == null) {
            p = new StreamPlayer(player.getUniqueId().toString());
            UserUtils.loadPlayer(p);
        }

        return p;
    }

    public Optional<StreamPlayer> getOrGetPlayer(CommandSender sender) {
        if (isConsole(sender)) {
            return UserUtils.getOrGetPlayer(GivenConfigs.getMainConfig().getConsoleDiscriminator());
        } else {
            return UserUtils.getOrGetPlayer(Streamline.getPlayer(sender).getUniqueId().toString());
        }
    }

    public String getUsername(CommandSender sender) {
        if (isConsole(sender)) return GivenConfigs.getMainConfig().getConsoleName();
        else return sender.getName();
    }

    @Override
    public String getUsername(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().getConsoleDiscriminator())) return GivenConfigs.getMainConfig().getConsoleName();
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

        SocketAddress address = player.getSocketAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        String ipSt = address.toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    @Override
    public boolean runAs(StreamPlayer user, boolean bypass, String command) {
        CommandSender source;
        if (user instanceof StreamPlayer) {
            StreamPlayer player = (StreamPlayer) user;
            source = Streamline.getPlayer(player.getUuid());
        }
        else {
            source = Streamline.getInstance().getProxy().getConsole();
            Streamline.getInstance().getProxy().getPluginManager().dispatchCommand(source, command);
            return true;
        }
        StreamPlayer player = (StreamPlayer) user;
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
    public ConcurrentSkipListSet<StreamPlayer> getUsersOn(String server) {
        ConcurrentSkipListSet<StreamPlayer> r = new ConcurrentSkipListSet<>();

        Streamline.getInstance().getProxy().getServers().values().forEach(a -> {
            a.getPlayers().forEach(b -> {
                r.add(this.getOrGetPlayer(b));
            });
        });

        return r;
    }

    @Override
    public void connect(StreamPlayer user, String server) {
        if (! user.isOnline()) return;

        ProxiedPlayer player = Streamline.getPlayer(user.getUuid());
        if (player == null) return;
        ServerInfo serverInfo = Streamline.getInstance().getProxy().getServerInfo(server);

        if (serverInfo == null) {
            MessageUtils.logWarning("Tried to send a user with uuid of '" + user.getUuid() + "' to server '" + server + "', but it does not exist!");
            return;
        }

        player.connect(serverInfo, ServerConnectEvent.Reason.PLUGIN);
    }

    @Override
    public void sendUserResourcePack(StreamPlayer user, StreamlineResourcePack pack) {
        if (! user.isOnline()) return;
        ProxiedPlayer p = Streamline.getPlayer(user.getUuid());
        if (p == null) return;
        StreamPlayer pl = getOrGetPlayer(p);

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
    public void kick(StreamPlayer user, String message) {
        ProxiedPlayer player = Streamline.getInstance().getProxy().getPlayer(user.getUuid());
        if (player == null) return;
        player.disconnect(Messenger.getInstance().codedText(message));
    }

    @Override
    public ProxiedPlayer getPlayer(String uuid) {
        return Streamline.getPlayer(uuid);
    }

    @Override
    public ConcurrentSkipListMap<String, StreamPlayer> ensurePlayers() {
        ConcurrentSkipListMap<String, StreamPlayer> r = new ConcurrentSkipListMap<>();

        for (ProxiedPlayer player : BasePlugin.onlinePlayers()) {
            if (UserUtils.isLoaded(player.getUniqueId().toString())) {
                StreamPlayer p = getOrGetPlayer(player);
                r.put(player.getUniqueId().toString(), p);
                continue;
            }

            StreamPlayer p = new StreamPlayer(player.getUniqueId().toString());
            r.put(player.getUniqueId().toString(), p);
        }

        return r;
    }

    @Override
    public String getServerPlayerIsOn(String uuid) {
        ProxiedPlayer player = getPlayer(uuid);
        if (player == null) return null;

        Server server = player.getServer();
        if (server == null) return null;

        ServerInfo info = server.getInfo();
        if (info == null) return null;

        return info.getName();
    }

    @Override
    public String getDisplayName(String uuid) {
        ProxiedPlayer player = getPlayer(uuid);
        if (player == null) return null;

        return player.getDisplayName();
    }
}
