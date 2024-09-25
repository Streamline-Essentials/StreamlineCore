package net.streamline.platform.savables;

import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.types.PermissionNode;
import net.streamline.api.SLAPI;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.platform.Messenger;
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
            public void chatAs(String command) {
                getPlayer().chat(command);
            }

            @Override
            public void runCommand(String command) {
                Bukkit.dispatchCommand(getPlayer(), command);
            }

            @Override
            public void sendMessage(String message) {
                getPlayer().sendMessage(Messenger.getInstance().codedString(message));
            }

            @Override
            public void sendMessageRaw(String message) {
                getPlayer().sendMessage(message);
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
