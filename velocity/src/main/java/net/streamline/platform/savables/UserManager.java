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
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.interfaces.IUserManager;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.StreamlineVelocity;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class UserManager implements IUserManager<CommandSource, Player> {
    @Getter
    private static UserManager instance;

    public UserManager() {
        instance = this;
    }

    @Override
    public StreamPlayer getOrCreatePlayer(Player player) {
        return UserUtils.getOrCreatePlayer(player.getUniqueId().toString());
    }

    @Override
    public StreamSender getOrCreateSender(CommandSource sender) {
        if (isConsole(sender)) {
            return UserUtils.getConsole();
        } else {
            Player player = (Player) sender;
            return getOrCreatePlayer(player);
        }
    }

    public String getUsername(CommandSource sender) {
        if (isConsole(sender)) return GivenConfigs.getMainConfig().getConsoleName();
        else return ((Player) sender).getUsername();
    }

    @Override
    public String getUsername(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().getConsoleDiscriminator())) return GivenConfigs.getMainConfig().getConsoleName();
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
    public boolean runAs(StreamPlayer user, boolean bypass, String command) {
        CommandSource source;
        if (user instanceof StreamPlayer) {
            StreamPlayer player = (StreamPlayer) user;
            source = StreamlineVelocity.getPlayer(player.getUuid());
        }
        else {
            source = StreamlineVelocity.getInstance().getProxy().getConsoleCommandSource();
            StreamlineVelocity.getInstance().getProxy().getCommandManager().executeImmediatelyAsync(source, command);
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
        StreamlineVelocity.getInstance().getProxy().getCommandManager().executeImmediatelyAsync(source, command);
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

        StreamlineVelocity.getInstance().getProxy().getAllServers().forEach(a -> {
            a.getPlayersConnected().forEach(b -> {
                StreamPlayer player = getOrCreatePlayer(b);
                if (player == null) return;
                if (player.isOnline() && player.getServerName().equals(server)) r.add(player);
            });
        });

        return r;
    }

    @Override
    public void connect(StreamPlayer user, String server) {
        if (! user.isOnline()) return;

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
    public void sendUserResourcePack(StreamPlayer player, StreamlineResourcePack pack) {
        if (! player.isOnline()) return;
        Player p = StreamlineVelocity.getPlayer(player.getUuid());
        if (p == null) return;

        try {
//            ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo(p.getUniqueId(), new URI(pack.getUrl()), pack.getHash());
//            p.sendResourcePackOffer(pack.getUrl(), pack.getHash(), pack.getPrompt(), pack.isForce());
            StreamlineVelocity.getInstance().sendResourcePack(pack, p);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    public void kick(StreamPlayer user, String message) {
        Optional<Player> playerOptional = StreamlineVelocity.getInstance().getProxy().getPlayer(user.getUuid());
        if (playerOptional.isEmpty()) return;
        playerOptional.get().disconnect(Messenger.getInstance().codedText(message));
    }

    @Override
    public Player getPlayer(String uuid) {
        return StreamlineVelocity.getPlayer(uuid);
    }

    @Override
    public ConcurrentSkipListMap<String, StreamPlayer> ensurePlayers() {
        ConcurrentSkipListMap<String, StreamPlayer> r = new ConcurrentSkipListMap<>();

        for (Player player : BasePlugin.onlinePlayers()) {
            r.put(player.getUniqueId().toString(), getOrCreatePlayer(player));
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
