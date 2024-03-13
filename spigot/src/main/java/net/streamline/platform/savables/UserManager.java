package net.streamline.platform.savables;

import lombok.Getter;
import net.luckperms.api.model.user.User;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.interfaces.IUserManager;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class UserManager implements IUserManager<CommandSender, Player> {
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
    public StreamSender getOrCreateSender(CommandSender sender) {
        if (isConsole(sender)) {
            return UserUtils.getConsole();
        } else {
            Player player = (Player) sender;
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
            Player player = Streamline.getPlayer(uuid);
            if (player == null) return null;
            return getUsername(player);
        }
    }

    public boolean isConsole(CommandSender sender) {
        return sender.equals(Bukkit.getConsoleSender());
    }

    @Override
    public boolean isOnline(String uuid) {
        if (UserUtils.isConsole(uuid)) return true;
        for (Player player : BasePlugin.onlinePlayers()) {
            if (player.getUniqueId().toString().equals(uuid)) return true;
        }

        return false;
    }

    public String parsePlayerIP(Player player) {
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();

        SocketAddress address = player.getAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        String ipSt = address.toString().replace("/", "");
        String[] ipSplit = ipSt.split(":");
        ipSt = ipSplit[0];

        return ipSt;
    }

    @Override
    public boolean runAs(StreamPlayer player, boolean bypass, String command) {
        CommandSender source;
        if (! player.isConsole()) {
            source = Streamline.getPlayer(player.getUuid());
        }
        else {
            source = Bukkit.getConsoleSender();
            Bukkit.dispatchCommand(source, command);
            return true;
        }
        if (source == null) return false;
        boolean already = source.hasPermission("*");
        if (bypass && !already) {
            User u = SLAPI.getLuckPerms().getUserManager().getUser(player.getUuid());
            if (u == null) return false;
            UserUtils.addPermission(u, "*");
        }
        Bukkit.dispatchCommand(source, command);
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

        for (Player player : BasePlugin.onlinePlayers()) {
            StreamPlayer p = getOrCreatePlayer(player);
            if (p.isOnline() && p.getServerName().equals(server)) r.add(p);
        }

        return r;
    }

    @Override
    public void connect(StreamPlayer user, String server) {
        // not applicable
    }

    @Override
    public void sendUserResourcePack(StreamPlayer user, StreamlineResourcePack pack) {
        if (! user.isOnline()) return;
        Player p = Streamline.getPlayer(user.getUuid());
        if (p == null) return;

        p.setResourcePack(pack.getUrl(), pack.getHash(), pack.getPrompt(), pack.isForce());
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

    @Override
    public void kick(StreamPlayer user, String message) {
        Player player = Streamline.getInstance().getProxy().getPlayer(user.getUuid());
        if (player == null) return;
        player.kickPlayer(Messenger.getInstance().codedString(message));
    }

    @Override
    public Player getPlayer(String uuid) {
        return Streamline.getPlayer(uuid);
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
    public String getServerPlayerIsOn(Player player) {
        return getServerPlayerIsOn(player.getUniqueId().toString());
    }

    @Override
    public String getServerPlayerIsOn(String uuid) {
        return "--null";
    }

    @Override
    public String getDisplayName(String uuid) {
        Player player = getPlayer(uuid);
        if (player == null) return null;

        return player.getDisplayName();
    }
}
