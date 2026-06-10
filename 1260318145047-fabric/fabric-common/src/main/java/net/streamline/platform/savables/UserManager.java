package net.streamline.platform.savables;

import lombok.Getter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.base.StreamlineFabric;
import net.streamline.platform.Messenger;
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
import java.util.UUID;
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
        if (sender instanceof ServerPlayer player) {
            return getOrCreatePlayer(player).map(s -> s);
        }
        if (sender instanceof CommandSourceStack stack && stack.getEntity() instanceof ServerPlayer player) {
            return getOrCreatePlayer(player).map(s -> s);
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
        SocketAddress address = player.connection.getRemoteAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        if (address instanceof InetSocketAddress inet) return inet.getHostString();
        return address.toString();
    }

    @Override
    public boolean runAs(CosmicSender player, boolean bypass, String command) {
        if (player.isConsole()) {
            if (StreamlineFabric.getInstance().getServer() == null) return false;
            StreamlineFabric.getInstance().getServer().getCommands()
                    .performPrefixedCommand(StreamlineFabric.getInstance().getServer().createCommandSourceStack(), command);
            return true;
        }
        ServerPlayer p = getPlayer(player.getUuid());
        if (p == null) return false;

        boolean already = false;
        if (bypass && !already) {
            if (LuckPermsHandler.hasLuckPerms()) {
                LuckPermsHandler.addPermission(player.getUuid(), "*");
            } else {
                return false;
            }
        }
        if (StreamlineFabric.getInstance().getServer() != null)
            StreamlineFabric.getInstance().getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack(), command);
        if (bypass && !already && LuckPermsHandler.hasLuckPerms()) {
            LuckPermsHandler.removePermission(player.getUuid(), "*");
        }
        return true;
    }

    @Override
    public ConcurrentSkipListSet<CosmicPlayer> getUsersOn(String server) {
        ConcurrentSkipListSet<CosmicPlayer> r = new ConcurrentSkipListSet<>();
        if (StreamlineFabric.getInstance().getServer() == null) return r;
        for (ServerPlayer player : StreamlineFabric.getInstance().getServer().getPlayerList().getPlayers()) {
            CosmicPlayer p = getOrCreatePlayer(player).orElse(null);
            if (p != null && p.isOnline() && p.getServerName().equals(server)) r.add(p);
        }
        return r;
    }

    @Override
    public void connect(CosmicPlayer user, String server) {
        // Not applicable on a Fabric backend
    }

    @Override
    public void sendUserResourcePack(CosmicPlayer user, CosmicResourcePack pack) {
        // Stub — resource pack sending via Fabric requires a network packet
    }

    @Override
    public double getPlayerPing(String uuid) {
        ServerPlayer player = getPlayer(uuid);
        if (player == null) return 0;
        // Latency not directly accessible; fallback to 0
        return 0;
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
        if (StreamlineFabric.getInstance().getServer() == null) return null;
        try {
            return StreamlineFabric.getInstance().getServer()
                    .getPlayerList().getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public ConcurrentSkipListMap<String, CosmicPlayer> ensurePlayers() {
        ConcurrentSkipListMap<String, CosmicPlayer> r = new ConcurrentSkipListMap<>();
        if (StreamlineFabric.getInstance().getServer() == null) return r;
        for (ServerPlayer player : StreamlineFabric.getInstance().getServer().getPlayerList().getPlayers()) {
            CosmicPlayer cp = getOrCreatePlayer(player).orElse(null);
            if (cp != null) r.put(player.getStringUUID(), cp);
        }
        return r;
    }

    @Override
    public String getServerPlayerIsOn(ServerPlayer player) {
        return "--null";
    }

    @Override
    public String getServerPlayerIsOn(String uuid) {
        return "--null";
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

        WorldPosition pos = location.getPosition();
        PlayerRotation rot = location.getRotation();

        p.teleportTo(pos.getX(), pos.getY(), pos.getZ());
        p.setYRot(rot.getYaw());
        p.setXRot(rot.getPitch());
    }

    @Override
    public void teleport(CosmicPlayer player, CosmicPlayer to) {
        ServerPlayer p = getPlayer(player.getUuid());
        ServerPlayer target = getPlayer(to.getUuid());
        if (p == null || target == null) return;
        p.teleportTo(target.getX(), target.getY(), target.getZ());
    }
}
