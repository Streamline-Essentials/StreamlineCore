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

/**
 * Spigot implementation of {@link IPlayerInterface} that resolves online Bukkit
 * {@link Player} instances and wraps them in cross-platform {@link RealPlayer} adapters.
 *
 * <p>Permission additions and removals are delegated to {@link LuckPermsHandler}.
 * Chat input, commands, and messaging all flow through the Bukkit {@link Sender} utility.
 */
@Getter @Setter
public class PlayerInterface implements IPlayerInterface<Player> {
    /**
     * {@inheritDoc}
     *
     * <p>Returns a {@link PlayerGetter} that looks up the online {@link Player} by UUID
     * via {@link Bukkit#getPlayer(UUID)}.
     *
     * @param uuid the player's unique identifier
     * @return a getter that resolves to the online Bukkit player, or {@code null} if offline
     */
    @Override
    public PlayerGetter<Player> getPlayerGetter(UUID uuid) {
        return () -> Bukkit.getPlayer(uuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a {@link PlayerGetter} that looks up the online {@link Player} by name
     * via {@link Bukkit#getPlayer(String)}.
     *
     * @param playerName the player's name (case-insensitive partial match)
     * @return a getter that resolves to the online Bukkit player, or {@code null} if not found
     */
    @Override
    public PlayerGetter<Player> getPlayerGetter(String playerName) {
        return () -> Bukkit.getPlayer(playerName);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Wraps the supplied {@link PlayerGetter} in a {@link RealPlayer} whose action
     * methods delegate to Bukkit {@link Sender} and {@link LuckPermsHandler}.
     *
     * @param playerGetter the supplier that resolves the underlying {@link Player}
     * @return a {@link RealPlayer} adapter for the resolved player
     */
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
