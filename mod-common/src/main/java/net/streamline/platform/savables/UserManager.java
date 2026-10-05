package net.streamline.platform.savables;

import lombok.Getter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;
import net.streamline.platform.handlers.GameplayHandler;
import singularity.Singularity;
import net.streamline.platform.compat.McCompat;
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

public class UserManager implements IUserManager<Object, ServerPlayer> {

    @Getter
    private static UserManager instance;

    public UserManager() {
        instance = this;
    }

    @Override
    public Optional<CosmicPlayer> getOrCreatePlayer(ServerPlayer player) {
        return UserUtils.getOrCreatePlayer(player.getStringUUID());
    }

    @Override
    public Optional<CosmicSender> getOrCreateSender(Object sender) {
        if (sender instanceof ServerPlayer) {
            return getOrCreatePlayer((ServerPlayer) sender).map(s -> s);
        }
        if (sender instanceof CommandSourceStack && ((CommandSourceStack) sender).getEntity() instanceof ServerPlayer) {
            return getOrCreatePlayer((ServerPlayer) ((CommandSourceStack) sender).getEntity()).map(s -> s);
        }
        return Optional.ofNullable(UserUtils.getConsole());
    }

    @Override
    public String getUsername(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().getConsoleDiscriminator()))
            return GivenConfigs.getMainConfig().getConsoleName();
        ServerPlayer player = getPlayer(uuid);
        if (player == null) return null;
        return player.getName().getString();
    }

    @Override
    public boolean isOnline(String uuid) {
        if (UserUtils.isConsole(uuid)) return true;
        return getPlayer(uuid) != null;
    }

    @Override
    public String parsePlayerIP(String uuid) {
        ServerPlayer player = getPlayer(uuid);
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        String ip = getIp(player);
        if (ip == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        return ip;
    }

    /**
     * The player's connection address as a host string, or {@code null} when it is unknown.
     */
    public static String getIp(ServerPlayer player) {
        SocketAddress address = player.connection.getRemoteAddress();
        if (address == null) return null;
        if (address instanceof InetSocketAddress) return ((InetSocketAddress) address).getHostString();
        return address.toString();
    }

    /**
     * Runs the command as the player. With {@code bypass}, the player holds the {@code *}
     * permission for the duration of the command, which requires LuckPerms.
     */
    @Override
    public boolean runAs(CosmicSender player, boolean bypass, String command) {
        MinecraftServer server = BasePlugin.getServer();
        if (server == null) return false;

        if (player.isConsole()) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            return true;
        }

        ServerPlayer p = getPlayer(player.getUuid());
        if (p == null) return false;

        if (bypass) {
            if (! PlayerInterface.hasLuckPerms()) return false;
            LuckPermsHandler.addPermission(player.getUuid(), "*");
        }
        try {
            server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), command);
        } finally {
            if (bypass) LuckPermsHandler.removePermission(player.getUuid(), "*");
        }
        return true;
    }

    @Override
    public ConcurrentSkipListSet<CosmicPlayer> getUsersOn(String server) {
        ConcurrentSkipListSet<CosmicPlayer> r = new ConcurrentSkipListSet<>();
        for (ServerPlayer player : BasePlugin.onlinePlayers()) {
            CosmicPlayer p = getOrCreatePlayer(player).orElse(null);
            if (p != null && p.isOnline() && p.getServerName().equals(server)) r.add(p);
        }
        return r;
    }

    /**
     * A backend server cannot move a player to another server; that is the proxy's job.
     */
    @Override
    public void connect(CosmicPlayer user, String server) {
    }

    @Override
    public void sendUserResourcePack(CosmicPlayer user, CosmicResourcePack pack) {
        BasePlugin.getInstance().sendResourcePack(pack, user);
    }

    @Override
    public double getPlayerPing(String uuid) {
        ServerPlayer player = getPlayer(uuid);
        if (player == null) return 0;
        return McCompat.getPing(player);
    }

    @Override
    public void kick(CosmicPlayer user, String message) {
        ServerPlayer player = getPlayer(user.getUuid());
        if (player == null) return;
        String coded = Messenger.getInstance() != null ? Messenger.getInstance().codedString(message) : message;
        player.connection.disconnect(Component.literal(coded));
    }

    @Override
    public ServerPlayer getPlayer(String uuid) {
        return BasePlugin.getPlayer(uuid);
    }

    @Override
    public ConcurrentSkipListMap<String, CosmicPlayer> ensurePlayers() {
        ConcurrentSkipListMap<String, CosmicPlayer> r = new ConcurrentSkipListMap<>();
        for (ServerPlayer player : BasePlugin.onlinePlayers()) {
            getOrCreatePlayer(player).ifPresent(cp -> r.put(player.getStringUUID(), cp));
        }
        return r;
    }

    /**
     * A mod-loader server is its own server, so an online player is on the server named
     * in {@code server-config.yml}; an offline player is on none.
     */
    @Override
    public String getServerPlayerIsOn(ServerPlayer player) {
        return player == null ? "--null" : Singularity.getServerName();
    }

    @Override
    public String getServerPlayerIsOn(String uuid) {
        return getServerPlayerIsOn(getPlayer(uuid));
    }

    @Override
    public String getDisplayName(String uuid) {
        ServerPlayer player = getPlayer(uuid);
        if (player == null) return null;
        return player.getDisplayName().getString();
    }

    @Override
    public void teleport(CosmicPlayer player, CosmicLocation location) {
        ServerPlayer p = getPlayer(player.getUuid());
        if (p == null) return;
        teleport(p, location);
    }

    @Override
    public void teleport(CosmicPlayer player, CosmicPlayer to) {
        ServerPlayer target = getPlayer(to.getUuid());
        if (target == null) return;
        Singularity.gameplay().ifPresent(gameplay -> gameplay.teleport(player.getUuid(), GameplayHandler.locationOf(target)));
    }

    /**
     * Moves the player to the location, changing dimension when needed. A location in a
     * dimension that does not exist leaves the player where they are.
     */
    public static void teleport(ServerPlayer player, CosmicLocation location) {
        if (Singularity.gameplay().isPresent()) {
            Singularity.gameplay().get().teleport(player.getStringUUID(), location);
            return;
        }

        WorldPosition pos = location.getPosition();
        PlayerRotation rot = location.getRotation();
        player.setYRot(rot.getYaw());
        player.setXRot(rot.getPitch());
        player.teleportTo(pos.getX(), pos.getY(), pos.getZ());
    }
}
