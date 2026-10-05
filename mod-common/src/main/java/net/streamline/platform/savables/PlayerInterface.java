package net.streamline.platform.savables;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;
import net.streamline.platform.compat.McCompat;
import singularity.permissions.DefaultPermissions;
import singularity.interfaces.audiences.IPlayerInterface;
import singularity.interfaces.audiences.getters.PlayerGetter;
import singularity.interfaces.audiences.real.RealPlayer;

import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class PlayerInterface implements IPlayerInterface<ServerPlayer> {

    @Override
    public PlayerGetter<ServerPlayer> getPlayerGetter(UUID uuid) {
        return () -> {
            MinecraftServer server = BasePlugin.getServer();
            return server != null ? server.getPlayerList().getPlayer(uuid) : null;
        };
    }

    @Override
    public PlayerGetter<ServerPlayer> getPlayerGetter(String playerName) {
        return () -> {
            MinecraftServer server = BasePlugin.getServer();
            return server != null ? server.getPlayerList().getPlayerByName(playerName) : null;
        };
    }

    @Override
    public RealPlayer<ServerPlayer> getPlayer(PlayerGetter<ServerPlayer> playerGetter) {
        return new RealPlayer<ServerPlayer>(playerGetter) {
            @Override
            public void chatAs(String message) {
                runCommand(message);
            }

            @Override
            public void runCommand(String command) {
                ServerPlayer player = getPlayer();
                MinecraftServer server = BasePlugin.getServer();
                if (player == null || server == null) return;
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), command);
            }

            @Override
            public void sendMessage(String message) {
                ServerPlayer player = getPlayer();
                if (player == null) return;
                Messenger messenger = Messenger.getInstance();
                player.sendSystemMessage(Component.literal(messenger != null ? messenger.codedString(message) : message));
            }

            @Override
            public void sendMessageRaw(String message) {
                ServerPlayer player = getPlayer();
                if (player == null) return;
                player.sendSystemMessage(Component.literal(message));
            }

            /**
             * Mod loaders have no common permission API. A value LuckPerms sets explicitly
             * wins; otherwise nodes modules declare as defaults are granted to everyone, and
             * everything else to operators (level 2).
             */
            @Override
            public boolean hasPermission(String permission) {
                ServerPlayer player = getPlayer();
                if (player == null) return false;
                Optional<Boolean> explicit = LuckPermsHandler.permissionValue(player.getStringUUID(), permission);
                if (explicit.isPresent()) return explicit.get();
                if (DefaultPermissions.isGrantedByDefault(permission)) return true;
                return McCompat.isOperator(player);
            }

            @Override
            public void addPermission(String permission) {
                ServerPlayer player = getPlayer();
                if (player == null) return;
                LuckPermsHandler.addPermission(player.getStringUUID(), permission);
            }

            @Override
            public void removePermission(String permission) {
                ServerPlayer player = getPlayer();
                if (player == null) return;
                LuckPermsHandler.removePermission(player.getStringUUID(), permission);
            }
        };
    }
}
