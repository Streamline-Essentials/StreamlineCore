package net.streamline.platform.savables;

import lombok.Getter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.streamline.api.SLAPI;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.base.StreamlineBungee;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.server.CosmicServer;
import singularity.interfaces.IUserManager;
import singularity.messages.builders.ResourcePackMessageBuilder;
import singularity.objects.CosmicResourcePack;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class UserManager implements IUserManager<CommandSender, ProxiedPlayer> {
    @Getter
    private static UserManager instance;

    public UserManager() {
        instance = this;
    }

    @Override
    public CosmicPlayer getOrCreatePlayer(ProxiedPlayer player) {
        return UserUtils.getOrCreatePlayer(player.getUniqueId().toString());
    }

    @Override
    public CosmicSender getOrCreateSender(CommandSender sender) {
        if (isConsole(sender)) {
            return UserUtils.getConsole();
        } else {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            return getOrCreatePlayer(player);
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
            ProxiedPlayer player = StreamlineBungee.getPlayer(uuid);
            if (player == null) return null;
            return getUsername(player);
        }
    }

    public boolean isConsole(CommandSender sender) {
        return ! (sender instanceof ProxiedPlayer);
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
    public boolean runAs(CosmicSender user, boolean bypass, String command) {
        CommandSender source;
        if (user instanceof CosmicPlayer) {
            CosmicPlayer player = (CosmicPlayer) user;
            source = StreamlineBungee.getPlayer(player.getUuid());
        }
        else {
            source = StreamlineBungee.getInstance().getProxy().getConsole();
            StreamlineBungee.getInstance().getProxy().getPluginManager().dispatchCommand(source, command);
            return true;
        }
        CosmicPlayer player = (CosmicPlayer) user;
        if (source == null) return false;
        boolean already = source.hasPermission("*");
        if (bypass && !already) {
            if (LuckPermsHandler.hasLuckPerms()) {
                LuckPermsHandler.addPermission(player.getUuid(), "*");
            } else {
                return false;
            }
        }
        StreamlineBungee.getInstance().getProxy().getPluginManager().dispatchCommand(source, command);
        if (bypass && !already) {
            if (LuckPermsHandler.hasLuckPerms()) {
                LuckPermsHandler.removePermission(player.getUuid(), "*");
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public ConcurrentSkipListSet<CosmicPlayer> getUsersOn(String server) {
        ConcurrentSkipListSet<CosmicPlayer> r = new ConcurrentSkipListSet<>();

        StreamlineBungee.getInstance().getProxy().getServers().values().forEach(a -> {
            a.getPlayers().forEach(b -> {
                CosmicPlayer player = getOrCreatePlayer(b);
                if (player == null) return;
                if (player.isOnline() && player.getServerName().equals(server)) r.add(player);
            });
        });

        return r;
    }

    @Override
    public void connect(CosmicPlayer user, String server) {
        if (! user.isOnline()) return;

        ProxiedPlayer player = StreamlineBungee.getPlayer(user.getUuid());
        if (player == null) return;
        ServerInfo serverInfo = StreamlineBungee.getInstance().getProxy().getServerInfo(server);

        if (serverInfo == null) {
            MessageUtils.logWarning("Tried to send a user with uuid of '" + user.getUuid() + "' to server '" + server + "', but it does not exist!");
            return;
        }

        player.connect(serverInfo, ServerConnectEvent.Reason.PLUGIN);
    }

    @Override
    public void sendUserResourcePack(CosmicPlayer user, CosmicResourcePack pack) {
        if (! user.isOnline()) return;
        ProxiedPlayer p = StreamlineBungee.getPlayer(user.getUuid());
        if (p == null) return;

        SLAPI.getInstance().getProxyMessenger().sendMessage(ResourcePackMessageBuilder.build(user, true, user, pack));
    }

    @Override
    public String parsePlayerIP(String uuid) {
        ProxiedPlayer player = StreamlineBungee.getPlayer(uuid);
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
        ProxiedPlayer player = StreamlineBungee.getPlayer(uuid);
        if (player == null) return 0d;
        return player.getPing();
    }

    @Override
    public void kick(CosmicPlayer user, String message) {
        ProxiedPlayer player = StreamlineBungee.getInstance().getProxy().getPlayer(user.getUuid());
        if (player == null) return;
        player.disconnect(Messenger.getInstance().codedText(message));
    }

    @Override
    public ProxiedPlayer getPlayer(String uuid) {
        return StreamlineBungee.getPlayer(uuid);
    }

    @Override
    public ConcurrentSkipListMap<String, CosmicPlayer> ensurePlayers() {
        ConcurrentSkipListMap<String, CosmicPlayer> r = new ConcurrentSkipListMap<>();

        for (ProxiedPlayer player : BasePlugin.onlinePlayers()) {
            if (UserUtils.isLoaded(player.getUniqueId().toString())) {
                r.put(player.getUniqueId().toString(), getOrCreatePlayer(player));
            }
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
    public String getServerPlayerIsOn(ProxiedPlayer player) {
        return getServerPlayerIsOn(player.getUniqueId().toString());
    }

    @Override
    public String getDisplayName(String uuid) {
        ProxiedPlayer player = getPlayer(uuid);
        if (player == null) return null;

        return player.getDisplayName();
    }

    @Override
    public void teleport(CosmicPlayer player, CosmicLocation location) {
        if (! player.isOnline()) return;
        ProxiedPlayer p = StreamlineBungee.getPlayer(player.getUuid());
        if (p == null) return;

        CosmicServer server = location.getServer();
        String serverName = server.getIdentifier();

        ServerInfo info = StreamlineBungee.getInstance().getProxy().getServerInfo(serverName);
        if (info == null) return;

        p.connect(info, ServerConnectEvent.Reason.PLUGIN);
    }
}
