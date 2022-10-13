package net.streamline.platform.savables;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
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
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    public StreamlineUser getOrGetUser(CommandSource sender) {
        if (isConsole(sender)) {
            return UserUtils.getOrGetUser(GivenConfigs.getMainConfig().userConsoleDiscriminator());
        } else {
            Player player = Streamline.getPlayer(sender);
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
            Player player = Streamline.getPlayer(uuid);
            if (player == null) return null;
            return getUsername(player);
        }
    }

    public boolean isConsole(CommandSource sender) {
        return sender.equals(Streamline.getInstance().getProxy().getConsoleCommandSource());
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
        if (user instanceof StreamlinePlayer player) source = Streamline.getPlayer(player.getUuid());
        else {
            source = Streamline.getInstance().getProxy().getConsoleCommandSource();
            Streamline.getInstance().getProxy().getCommandManager().executeImmediatelyAsync(source, command);
            return true;
        }
        if (source == null) return false;
        boolean already = source.hasPermission("*");
        if (bypass && !already) {
            User u = SLAPI.getLuckPerms().getUserManager().getUser(player.getUuid());
            if (u == null) return false;
            UserUtils.addPermission(u, "*");
        }
        Streamline.getInstance().getProxy().getCommandManager().executeImmediatelyAsync(source, command);
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

        Streamline.getInstance().getProxy().getAllServers().forEach(a -> {
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

        Player player = Streamline.getPlayer(user.getUuid());
        if (player == null) return;
        Optional<RegisteredServer> serverOptional = Streamline.getInstance().getProxy().getServer(server);

        if (serverOptional.isEmpty()) {
            MessageUtils.logWarning("Tried to send a user with uuid of '" + user.getUuid() + "' to server '" + server + "', but it does not exist!");
            return;
        }

        player.createConnectionRequest(serverOptional.get());
    }

    @Override
    public void sendUserResourcePack(StreamlineUser user, StreamlineResourcePack pack) {
        if (! (user instanceof StreamlinePlayer player)) return;
        if (! player.updateOnline()) return;
        Player p = Streamline.getPlayer(user.getUuid());
        if (p == null) return;

        Streamline.getInstance().sendResourcePack(pack, user);
    }

    @Override
    public String parsePlayerIP(String uuid) {
        Player player = Streamline.getPlayer(uuid);
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
        Player player = Streamline.getPlayer(uuid);
        if (player == null) return 0d;
        return player.getPing();
    }

    @Override
    public void kick(StreamlineUser user, String message) {
        Optional<Player> playerOptional = Streamline.getInstance().getProxy().getPlayer(user.getUuid());
        if (playerOptional.isEmpty()) return;
        playerOptional.get().disconnect(Messenger.getInstance().codedText(message));
    }
}
