package net.streamline.platform.savables;

import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.base.StreamlineVelocity;
import net.streamline.platform.Messenger;
import singularity.interfaces.audiences.IPlayerInterface;
import singularity.interfaces.audiences.getters.PlayerGetter;
import singularity.interfaces.audiences.real.RealPlayer;

import java.util.UUID;

@Getter @Setter
public class PlayerInterface implements IPlayerInterface<Player> {
    @Override
    public PlayerGetter<Player> getPlayerGetter(UUID uuid) {
        return () -> StreamlineVelocity.getPlayer(uuid);
    }

    @Override
    public PlayerGetter<Player> getPlayerGetter(String playerName) {
        return () -> StreamlineVelocity.getPlayer(playerName);
    }

    @Override
    public RealPlayer<Player> getPlayer(PlayerGetter<Player> playerGetter) {
        return new RealPlayer<>(playerGetter) {
            @Override
            public void chatAs(String command) {
                getPlayer().spoofChatInput(command);
            }

            @Override
            public void runCommand(String command) {
                StreamlineVelocity.getInstance().getProxy().getCommandManager().executeAsync(getPlayer(), command);
            }

            @Override
            public void sendMessage(String message) {
                getPlayer().sendMessage(Messenger.getInstance().codedText(message));
            }

            @Override
            public void sendMessageRaw(String message) {
                getPlayer().sendMessage(Component.text(message));
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
