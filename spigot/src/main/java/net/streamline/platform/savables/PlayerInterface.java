package net.streamline.platform.savables;

import host.plas.bou.commands.Sender;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.permissions.LuckPermsHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import singularity.interfaces.audiences.IPlayerInterface;
import singularity.interfaces.audiences.getters.PlayerGetter;
import singularity.interfaces.audiences.real.RealPlayer;

import java.util.UUID;

@Getter @Setter
public class PlayerInterface implements IPlayerInterface<Player> {
    @Override
    public PlayerGetter<Player> getPlayerGetter(UUID uuid) {
        return () -> Bukkit.getPlayer(uuid);
    }

    @Override
    public PlayerGetter<Player> getPlayerGetter(String playerName) {
        return () -> Bukkit.getPlayer(playerName);
    }

    @Override
    public RealPlayer<Player> getPlayer(PlayerGetter<Player> playerGetter) {
        return new RealPlayer<>(playerGetter) {
            @Override
            public void chatAs(String message) {
                new Sender(getPlayer()).chatAs(message);
            }

            @Override
            public void runCommand(String command) {
                new Sender(getPlayer()).executeCommand(command);
            }

            @Override
            public void sendMessage(String message) {
                new Sender(getPlayer()).sendMessage(message);
            }

            @Override
            public void sendMessageRaw(String message) {
                new Sender(getPlayer()).sendMessage(message, false);
            }

            @Override
            public boolean hasPermission(String permission) {
                return getPlayer().hasPermission(permission);
            }

            @Override
            public void addPermission(String permission) {
                LuckPermsHandler.addPermission(getPlayer().getUniqueId().toString(), permission);
            }

            @Override
            public void removePermission(String permission) {
                LuckPermsHandler.removePermission(getPlayer().getUniqueId().toString(), permission);
            }
        };
    }
}
