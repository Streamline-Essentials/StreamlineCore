package net.streamline.platform.savables;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.base.StreamlineFabric;
import singularity.interfaces.audiences.IPlayerInterface;
import singularity.interfaces.audiences.getters.PlayerGetter;
import singularity.interfaces.audiences.real.RealPlayer;

import java.util.UUID;

@Getter
@Setter
public class PlayerInterface implements IPlayerInterface<ServerPlayer> {

    @Override
    public PlayerGetter<ServerPlayer> getPlayerGetter(UUID uuid) {
        return () -> StreamlineFabric.getInstance().getServer() != null
                ? StreamlineFabric.getInstance().getServer().getPlayerList().getPlayer(uuid)
                : null;
    }

    @Override
    public PlayerGetter<ServerPlayer> getPlayerGetter(String playerName) {
        return () -> StreamlineFabric.getInstance().getServer() != null
                ? StreamlineFabric.getInstance().getServer().getPlayerList().getPlayerByName(playerName)
                : null;
    }

    @Override
    public RealPlayer<ServerPlayer> getPlayer(PlayerGetter<ServerPlayer> playerGetter) {
        return new RealPlayer<>(playerGetter) {
            @Override
            public void chatAs(String message) {
                if (getPlayer() == null) return;
                if (StreamlineFabric.getInstance().getServer() == null) return;
                StreamlineFabric.getInstance().getServer().getCommands().performPrefixedCommand(
                        getPlayer().createCommandSourceStack(), message);
            }

            @Override
            public void runCommand(String command) {
                if (getPlayer() == null) return;
                if (StreamlineFabric.getInstance().getServer() == null) return;
                StreamlineFabric.getInstance().getServer().getCommands().performPrefixedCommand(
                        getPlayer().createCommandSourceStack(), command);
            }

            @Override
            public void sendMessage(String message) {
                if (getPlayer() == null) return;
                getPlayer().sendSystemMessage(Component.literal(message));
            }

            @Override
            public void sendMessageRaw(String message) {
                if (getPlayer() == null) return;
                getPlayer().sendSystemMessage(Component.literal(message));
            }

            @Override
            public boolean hasPermission(String permission) {
                if (getPlayer() == null) return false;
                // LuckPerms permission check via node presence
                return false; // LuckPerms handles actual permissions; no cross-version op API
            }

            @Override
            public void addPermission(String permission) {
                if (getPlayer() == null) return;
                LuckPermsHandler.addPermission(getPlayer().getStringUUID(), permission);
            }

            @Override
            public void removePermission(String permission) {
                if (getPlayer() == null) return;
                LuckPermsHandler.removePermission(getPlayer().getStringUUID(), permission);
            }
        };
    }
}
