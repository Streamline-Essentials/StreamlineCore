package net.streamline.platform.savables;

import host.plas.bou.scheduling.TaskManager;
import lombok.Getter;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.base.StreamlineSpigot;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.WorldPosition;
import singularity.interfaces.IUserManager;
import singularity.objects.CosmicResourcePack;
import singularity.utils.UserUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class UserManager implements IUserManager<CommandSender, Player> {
    @Getter
    private static UserManager instance;

    public UserManager() {
        instance = this;
    }

    @Override
    public Optional<CosmicPlayer> getOrCreatePlayer(Player player) {
        return UserUtils.getOrCreatePlayer(player.getUniqueId().toString());
    }

    @Override
    public Optional<CosmicSender> getOrCreateSender(CommandSender sender) {
        if (isConsole(sender)) {
            return Optional.ofNullable(UserUtils.getConsole());
        } else {
            Player player = (Player) sender;
            return getOrCreatePlayer(player).map(s -> s);
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
            Player player = StreamlineSpigot.getPlayer(uuid);
            if (player == null) return null;
            return getUsername(player);
        }
    }

    public boolean isConsole(CommandSender sender) {
        return ! (sender instanceof Player);
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
    public boolean runAs(CosmicSender player, boolean bypass, String command) {
        CommandSender source;
        if (! player.isConsole()) {
            source = StreamlineSpigot.getPlayer(player.getUuid());
        }
        else {
            source = Bukkit.getConsoleSender();
            Bukkit.dispatchCommand(source, command);
            return true;
        }
        if (source == null) return false;
        boolean already = source.hasPermission("*");
        if (bypass && !already) {
            if (LuckPermsHandler.hasLuckPerms()) {
                LuckPermsHandler.addPermission(player.getUuid(), "*");
            } else {
                return false;
            }
        }
        Bukkit.dispatchCommand(source, command);
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

        for (Player player : BasePlugin.onlinePlayers()) {
            CosmicPlayer p = getOrCreatePlayer(player).orElse(null);
            if (p == null) continue;
            if (p.isOnline() && p.getServerName().equals(server)) r.add(p);
        }

        return r;
    }

    @Override
    public void connect(CosmicPlayer user, String server) {
        // not applicable
    }

    @Override
    public void sendUserResourcePack(CosmicPlayer user, CosmicResourcePack pack) {
        if (! user.isOnline()) return;
        Player p = StreamlineSpigot.getPlayer(user.getUuid());
        if (p == null) return;

//        p.setResourcePack(pack.getUrl(), pack.getHash(), pack.getPrompt(), pack.isForce());
        p.setResourcePack(pack.getUrl(), pack.getHash());
    }

    @Override
    public String parsePlayerIP(String uuid) {
        Player player = StreamlineSpigot.getPlayer(uuid);
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
        Player player = StreamlineSpigot.getPlayer(uuid);
        if (player == null) return 0d;
        return player.getPing();
    }

    @Override
    public void kick(CosmicPlayer user, String message) {
        Player player = StreamlineSpigot.getInstance().getProxy().getPlayer(user.getUuid());
        if (player == null) return;
        player.kickPlayer(Messenger.getInstance().codedString(message));
    }

    @Override
    public Player getPlayer(String uuid) {
        return StreamlineSpigot.getPlayer(uuid);
    }

    @Override
    public ConcurrentSkipListMap<String, CosmicPlayer> ensurePlayers() {
        ConcurrentSkipListMap<String, CosmicPlayer> r = new ConcurrentSkipListMap<>();

        for (Player player : BasePlugin.onlinePlayers()) {
            CosmicPlayer cosmicPlayer = getOrCreatePlayer(player).orElse(null);
            if (cosmicPlayer == null) continue;
            r.put(player.getUniqueId().toString(), cosmicPlayer);
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

    @Override
    public void teleport(CosmicPlayer player, CosmicLocation location) {
        if (! player.isOnline()) return;
        Player p = StreamlineSpigot.getPlayer(player.getUuid());
        if (p == null) return;

        WorldPosition pos = location.getPosition();
        World world = Bukkit.getWorld(location.getWorldName());
        if (world == null) return;
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        PlayerRotation rot = location.getRotation();
        float yaw = rot.getYaw();
        float pitch = rot.getPitch();

        Location loc = new Location(world, x, y, z, yaw, pitch);

        TaskManager.teleport(p, loc);
    }
}
