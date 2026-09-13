package net.streamline.platform.savables;

import lombok.Getter;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.base.StreamlineForge;
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

public class UserManager implements IUserManager<Object, ServerPlayerEntity> {

    @Getter
    private static UserManager instance;

    public UserManager() {
        instance = this;
    }

    @Override
    public Optional<CosmicPlayer> getOrCreatePlayer(ServerPlayerEntity player) {
        return UserUtils.getOrCreatePlayer(player.getStringUUID());
    }

    @Override
    public Optional<CosmicSender> getOrCreateSender(Object sender) {
        if (sender instanceof ServerPlayerEntity) {
            return getOrCreatePlayer((ServerPlayerEntity) sender).map(s -> s);
        }
        if (sender instanceof CommandSource) {
            CommandSource stack = (CommandSource) sender;
            if (stack.getEntity() instanceof ServerPlayerEntity) {
                return getOrCreatePlayer((ServerPlayerEntity) stack.getEntity()).map(s -> s);
            }
        }
        return Optional.ofNullable(UserUtils.getConsole());
    }

    @Override
    public String getUsername(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().getConsoleDiscriminator()))
            return GivenConfigs.getMainConfig().getConsoleName();
        ServerPlayerEntity player = getPlayer(uuid);
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
        ServerPlayerEntity player = getPlayer(uuid);
        if (player == null) return MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get();
        SocketAddress address = player.connection.connection.channel().remoteAddress();
        if (address == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
        if (address instanceof InetSocketAddress) return ((InetSocketAddress) address).getHostString();
        return address.toString();
    }

    @Override
    public boolean runAs(CosmicSender player, boolean bypass, String command) {
        if (player.isConsole()) {
            if (ServerLifecycleHooks.getCurrentServer() == null) return false;
            ServerLifecycleHooks.getCurrentServer().getCommands()
                    .performCommand(ServerLifecycleHooks.getCurrentServer().createCommandSourceStack(), command);
            return true;
        }
        ServerPlayerEntity p = getPlayer(player.getUuid());
        if (p == null) return false;

        boolean already = false;
        if (bypass && !already) {
            if (LuckPermsHandler.hasLuckPerms()) {
                LuckPermsHandler.addPermission(player.getUuid(), "*");
            } else {
                return false;
            }
        }
        p.getServer().getCommands().performCommand(p.createCommandSourceStack(), command);
        if (bypass && !already && LuckPermsHandler.hasLuckPerms()) {
            LuckPermsHandler.removePermission(player.getUuid(), "*");
        }
        return true;
    }

    @Override
    public ConcurrentSkipListSet<CosmicPlayer> getUsersOn(String server) {
        ConcurrentSkipListSet<CosmicPlayer> r = new ConcurrentSkipListSet<>();
        if (ServerLifecycleHooks.getCurrentServer() == null) return r;
        for (ServerPlayerEntity player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            CosmicPlayer p = getOrCreatePlayer(player).orElse(null);
            if (p != null && p.isOnline() && p.getServerName().equals(server)) r.add(p);
        }
        return r;
    }

    @Override
    public void connect(CosmicPlayer user, String server) {
        // Not applicable on a Forge backend
    }

    @Override
    public void sendUserResourcePack(CosmicPlayer user, CosmicResourcePack pack) {
        // Stub — resource pack sending via Forge requires a network packet
    }

    @Override
    public double getPlayerPing(String uuid) {
        ServerPlayerEntity player = getPlayer(uuid);
        if (player == null) return 0;
        return 0; // latency field not directly accessible via mapped API
    }

    @Override
    public void kick(CosmicPlayer user, String message) {
        ServerPlayerEntity player = getPlayer(user.getUuid());
        if (player == null) return;
        String coded = Messenger.getInstance() != null ? Messenger.getInstance().codedString(message) : message;
        player.connection.disconnect(new StringTextComponent(coded));
    }

    @Override
    public ServerPlayerEntity getPlayer(String uuid) {
        if (ServerLifecycleHooks.getCurrentServer() == null) return null;
        try {
            return ServerLifecycleHooks.getCurrentServer()
                    .getPlayerList().getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public ConcurrentSkipListMap<String, CosmicPlayer> ensurePlayers() {
        ConcurrentSkipListMap<String, CosmicPlayer> r = new ConcurrentSkipListMap<>();
        if (ServerLifecycleHooks.getCurrentServer() == null) return r;
        for (ServerPlayerEntity player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            CosmicPlayer cp = getOrCreatePlayer(player).orElse(null);
            if (cp != null) r.put(player.getStringUUID(), cp);
        }
        return r;
    }

    @Override
    public String getServerPlayerIsOn(ServerPlayerEntity player) {
        return "--null";
    }

    @Override
    public String getServerPlayerIsOn(String uuid) {
        return "--null";
    }

    @Override
    public String getDisplayName(String uuid) {
        ServerPlayerEntity player = getPlayer(uuid);
        if (player == null) return null;
        return player.getDisplayName().getString();
    }

    @Override
    public void teleport(CosmicPlayer player, CosmicLocation location) {
        ServerPlayerEntity p = getPlayer(player.getUuid());
        if (p == null) return;

        WorldPosition pos = location.getPosition();
        PlayerRotation rot = location.getRotation();

        ServerWorld world = (ServerWorld) ServerLifecycleHooks.getCurrentServer().overworld();
        p.teleportTo(world, pos.getX(), pos.getY(), pos.getZ(), rot.getYaw(), rot.getPitch());
    }

    @Override
    public void teleport(CosmicPlayer player, CosmicPlayer to) {
        ServerPlayerEntity p = getPlayer(player.getUuid());
        ServerPlayerEntity target = getPlayer(to.getUuid());
        if (p == null || target == null) return;
        p.teleportTo((ServerWorld) target.level, target.getX(), target.getY(), target.getZ(),
                target.yRot, target.xRot);
    }
}
