package net.streamline.platform.savables;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.streamline.api.permissions.LuckPermsHandler;
import singularity.interfaces.audiences.IPlayerInterface;
import singularity.interfaces.audiences.getters.PlayerGetter;
import singularity.interfaces.audiences.real.RealPlayer;

import java.util.UUID;

@Getter
@Setter
public class PlayerInterface implements IPlayerInterface<ServerPlayerEntity> {

    @Override
    public PlayerGetter<ServerPlayerEntity> getPlayerGetter(UUID uuid) {
        return () -> ServerLifecycleHooks.getCurrentServer() != null
                ? ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid)
                : null;
    }

    @Override
    public PlayerGetter<ServerPlayerEntity> getPlayerGetter(String playerName) {
        return () -> ServerLifecycleHooks.getCurrentServer() != null
                ? ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName)
                : null;
    }

    @Override
    public RealPlayer<ServerPlayerEntity> getPlayer(PlayerGetter<ServerPlayerEntity> playerGetter) {
        return new RealPlayer<ServerPlayerEntity>(playerGetter) {
            @Override
            public void chatAs(String message) {
                if (getPlayer() == null) return;
                getPlayer().getServer().getCommands().performCommand(
                        getPlayer().createCommandSourceStack(), message);
            }

            @Override
            public void runCommand(String command) {
                if (getPlayer() == null) return;
                getPlayer().getServer().getCommands().performCommand(
                        getPlayer().createCommandSourceStack(), command);
            }

            @Override
            public void sendMessage(String message) {
                if (getPlayer() == null) return;
                getPlayer().sendMessage(new StringTextComponent(message), getPlayer().getUUID());
            }

            @Override
            public void sendMessageRaw(String message) {
                if (getPlayer() == null) return;
                getPlayer().sendMessage(new StringTextComponent(message), getPlayer().getUUID());
            }

            @Override
            public boolean hasPermission(String permission) {
                if (getPlayer() == null) return false;
                return getPlayer().hasPermissions(4); // fallback: op check
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
