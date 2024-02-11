package net.streamline.platform.savables;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import lombok.Getter;
import net.luckperms.api.model.user.User;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.interfaces.IUserManager;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.StreamlineVelocity;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class UserManager implements IUserManager<Player> {
    @Getter
    private static UserManager instance;

    public UserManager() {
        instance = this;
    }

    public StreamlinePlayer getOrGetPlayer(Player player) {
        StreamlinePlayer p = UserUtils.getOrGetPlayer(player.getUniqueId().toString());
        if (p == null) {
            p = new StreamlinePlayer(player.getUniqueId().toString());
            UserUtils.loadUser(p);
        }

        return p;
    }

    public StreamlineUser getOrGetUser(CommandSource sender) {
        if (isConsole(sender)) {
            return UserUtils.getOrGetUser(GivenConfigs.getMainConfig().userConsoleDiscriminator());
        } else {
            Player player = StreamlineVelocity.getPlayer(sender);
            if (player == null) return null;
            return UserUtils.getOrGetUser(player.getUniqueId().toString());
        }
    }

    public String getUsername(CommandSource sender) {
        if (isConsole(sender)) return GivenConfigs.getMainConfig().userConsoleNameRegular();
        else return ((Player) sender).getUsername();
    }

    @Override
    public String getUsername(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().userConsoleDiscriminator())) return GivenConfigs.getMainConfig().userConsoleNameRegular();
        else {
            Player player = StreamlineVelocity.getPlayer(uuid);
            if (player == null) return null;
            return getUsername(player);
        }
    }

    public boolean isConsole(CommandSource sender) {
        return sender.equals(StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource());
    }

    @Override
    public boolean isOnline(String uuid) {
        if (UserUtils.isConsole(uuid)) return true;
        for (Player player : BasePlugin.onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return true;
        }

        return false;
    }

    @Override
    public boolean runAs(StreamlineUser user, boolean bypass, String command) {
        CommandSource source;
        if (user instanceof StreamlinePlayer) {
            StreamlinePlayer player = (StreamlinePlayer) user;
            source = StreamlineVelocity.getPlayer(player.getUuid());
        }
        else {
            source = StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource();
            StreamlineVelocity.getInstance().getProxy().getCommandManager().executeImmediatelyAsync(source, command);
            return true;
        }
        StreamlinePlayer player = (StreamlinePlayer) user;
        if (source == null) return false;
        boolean already = source.hasPermission("*");
        if (bypass && !already) {
            User u = SLAPI.getLuckPerms().getUserManager().getUser(player.getUuid());
            if (u == null) return false;
            UserUtils.addPermission(u, "*");
        }
        StreamlineVelocity.getInstance().getProxy().getCommandManager().executeImmediatelyAsync(source, command);
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

        StreamlineVelocity.getInstance().getProxy().getAllServers().forEach(a -> {
            a.getPlayersConnected().forEach(b -> {
                r.add(getOrGetUser(b));
            });
        });

        return r;
    }

    @Override
    public void connect(StreamlineUser user, String server) {
        if (! user.isOnline()) return;
        if (user instanceof StreamlineConsole) return;

        Player player = StreamlineVelocity.getPlayer(user.getUuid());
        if (player == null) return;
        Optional<RegisteredServer> serverOptional = StreamlineVelocity.getInstance().getProxy().getServer(server);

        if (serverOptional.isEmpty()) {
            MessageUtils.logWarning("Tried to send a user with uuid of '" + user.getUuid() + "' to server '" + server + "', but it does not exist!");
            return;
        }

        player.createConnectionRequest(serverOptional.get()).connect();
    }

    @Override
    public void sendUserResourcePack(StreamlineUser user, StreamlineResourcePack pack) {
        if (! (user instanceof StreamlinePlayer)) return;
        StreamlinePlayer player = (StreamlinePlayer) user;
        if (! player.updateOnline()) return;
        Player p = StreamlineVelocity.getPlayer(user.getUuid());
        if (p == null) return;

        StreamlineVelocity.getInstance().sendResourcePack(pack, user);
    }

    @Override
    public String parsePlayerIP(String uuid) {
        Player player = StreamlineVelocity.getPlayer(uuid);
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        InetSocketAddress address = player.getRemoteAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        String ipSt = address.toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    @Override
    public double getPlayerPing(String uuid) {
        Player player = StreamlineVelocity.getPlayer(uuid);
        if (player == null) return 0d;
        return player.getPing();
    }

    @Override
    public void kick(StreamlineUser user, String message) {
        Optional<Player> playerOptional = StreamlineVelocity.getInstance().getProxy().getPlayer(user.getUuid());
        if (playerOptional.isEmpty()) return;
        playerOptional.get().disconnect(Messenger.getInstance().codedText(message));
    }

    @Override
    public Player getPlayer(String uuid) {
        return StreamlineVelocity.getPlayer(uuid);
    }

    @Override
    public ConcurrentSkipListMap<String, StreamlineUser> ensurePlayers() {
        ConcurrentSkipListMap<String, StreamlineUser> r = new ConcurrentSkipListMap<>();

        for (Player player : BasePlugin.onlinePlayers()) {
            if (UserUtils.isLoaded(player.getUniqueId().toString())) {
                StreamlinePlayer p = getOrGetPlayer(player);
                r.put(player.getUniqueId().toString(), p);
                continue;
            }

            StreamlinePlayer p = new StreamlinePlayer(player.getUniqueId().toString());
            r.put(player.getUniqueId().toString(), p);
        }

        return r;
    }

    @Override
    public String getServerPlayerIsOn(String uuid) {
        Player player = getPlayer(uuid);
        if (player == null) return null;

        ServerConnection server = player.getCurrentServer().orElse(null);
        if (server == null) return null;

        ServerInfo info = server.getServerInfo();
        if (info == null) return null;

        return info.getName();
    }

    @Override
    public String getDisplayName(String uuid) {
        Player player = getPlayer(uuid);
        if (player == null) return null;

        return player.getUsername();
    }
}
