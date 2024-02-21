package net.streamline.platform.savables;

import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.types.PermissionNode;
import net.streamline.api.SLAPI;
import net.streamline.api.interfaces.audiences.IPlayerInterface;
import net.streamline.api.interfaces.audiences.getters.PlayerGetter;
import net.streamline.api.interfaces.audiences.real.RealPlayer;
import net.streamline.platform.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
                LuckPerms luckPerms = SLAPI.getLuckPerms();
                if (luckPerms == null) return;

                luckPerms.getUserManager().modifyUser(getPlayer().getUniqueId(), user -> user.data().add(PermissionNode.builder(permission).build()));
            }

            @Override
            public void removePermission(String permission) {
                LuckPerms luckPerms = SLAPI.getLuckPerms();
                if (luckPerms == null) return;

                luckPerms.getUserManager().modifyUser(getPlayer().getUniqueId(), user -> user.data().remove(PermissionNode.builder(permission).build()));
            }
        };
    }
}
