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

/**
 * Velocity implementation of {@link IPlayerInterface} that resolves online Velocity
 * {@link Player} instances and wraps them in cross-platform {@link RealPlayer} adapters.
 *
 * <p>Chat input is spoofed via {@link Player#spoofChatInput(String)}, command execution
 * is delegated to the Velocity {@link com.velocitypowered.api.command.CommandManager},
 * and permission changes are applied through {@link LuckPermsHandler}.
 */
@Getter @Setter
public class PlayerInterface implements IPlayerInterface<Player> {
    /**
     * {@inheritDoc}
     *
     * <p>Returns a {@link PlayerGetter} that resolves the online Velocity {@link Player}
     * by UUID via {@link StreamlineVelocity#getPlayer(UUID)}.
     *
     * @param uuid the player's unique identifier
     * @return a getter that resolves to the online Velocity player, or {@code null} if offline
     */
    @Override
    public PlayerGetter<Player> getPlayerGetter(UUID uuid) {
        return () -> StreamlineVelocity.getPlayer(uuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a {@link PlayerGetter} that resolves the online Velocity {@link Player}
     * by name via {@link StreamlineVelocity#getPlayer(String)}.
     *
     * @param playerName the player's username (UUID string)
     * @return a getter that resolves to the online Velocity player, or {@code null} if not found
     */
    @Override
    public PlayerGetter<Player> getPlayerGetter(String playerName) {
        return () -> StreamlineVelocity.getPlayer(playerName);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Wraps the supplied {@link PlayerGetter} in a {@link RealPlayer} whose action
     * methods delegate to the Velocity API and {@link LuckPermsHandler}.
     *
     * @param playerGetter the supplier that resolves the underlying {@link Player}
     * @return a {@link RealPlayer} adapter for the resolved player
     */
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
