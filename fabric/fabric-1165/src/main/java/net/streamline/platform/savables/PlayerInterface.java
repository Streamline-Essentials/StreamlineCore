package net.streamline.platform.savables;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.base.StreamlineFabric;
import singularity.interfaces.audiences.IPlayerInterface;
import singularity.interfaces.audiences.getters.PlayerGetter;
import singularity.interfaces.audiences.real.RealPlayer;

import java.util.UUID;

@Getter
@Setter
public class PlayerInterface implements IPlayerInterface<ServerPlayerEntity> {

    @Override
    public PlayerGetter<ServerPlayerEntity> getPlayerGetter(UUID uuid) {
        return () -> StreamlineFabric.getInstance().getServer() != null
                ? StreamlineFabric.getInstance().getServer().getPlayerManager().getPlayer(uuid)
                : null;
    }

    @Override
    public PlayerGetter<ServerPlayerEntity> getPlayerGetter(String playerName) {
        return () -> StreamlineFabric.getInstance().getServer() != null
                ? StreamlineFabric.getInstance().getServer().getPlayerManager().getPlayer(playerName)
                : null;
    }

    @Override
    public RealPlayer<ServerPlayerEntity> getPlayer(PlayerGetter<ServerPlayerEntity> playerGetter) {
        return new RealPlayer<ServerPlayerEntity>(playerGetter) {
            @Override
            public void chatAs(String message) {
                if (getPlayer() == null) return;
                getPlayer().getServer().getCommandManager().execute(
                        getPlayer().getCommandSource(), message);
            }

            @Override
            public void runCommand(String command) {
                if (getPlayer() == null) return;
                getPlayer().getServer().getCommandManager().execute(
                        getPlayer().getCommandSource(), command);
            }

            @Override
            public void sendMessage(String message) {
                if (getPlayer() == null) return;
                getPlayer().sendMessage(new LiteralText(message), false);
            }

            @Override
            public void sendMessageRaw(String message) {
                if (getPlayer() == null) return;
                getPlayer().sendMessage(new LiteralText(message), false);
            }

            @Override
            public boolean hasPermission(String permission) {
                if (getPlayer() == null) return false;
                return getPlayer().hasPermissionLevel(4);
            }

            @Override
            public void addPermission(String permission) {
                if (getPlayer() == null) return;
                LuckPermsHandler.addPermission(getPlayer().getUuidAsString(), permission);
            }

            @Override
            public void removePermission(String permission) {
                if (getPlayer() == null) return;
                LuckPermsHandler.removePermission(getPlayer().getUuidAsString(), permission);
            }
        };
    }
}
