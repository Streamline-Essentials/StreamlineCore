package net.streamline.platform.savables;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.streamline.api.permissions.LuckPermsHandler;
import net.streamline.platform.Messenger;
import singularity.interfaces.audiences.IPlayerInterface;
import singularity.interfaces.audiences.getters.PlayerGetter;
import singularity.interfaces.audiences.real.RealPlayer;

import java.util.UUID;

/**
 * BungeeCord implementation of {@link IPlayerInterface} that creates
 * {@link PlayerGetter}s and {@link RealPlayer} wrappers for
 * {@link ProxiedPlayer} instances.
 *
 * <p>Permission mutations are delegated to {@link LuckPermsHandler}; message
 * sending uses {@link Messenger#codedText} for colour processing.
 */
@Getter @Setter
public class PlayerInterface implements IPlayerInterface<ProxiedPlayer> {

    /**
     * {@inheritDoc}
     *
     * <p>Returns a {@link PlayerGetter} that resolves the player by UUID from
     * the BungeeCord proxy.
     */
    @Override
    public PlayerGetter<ProxiedPlayer> getPlayerGetter(UUID uuid) {
        return () -> ProxyServer.getInstance().getPlayer(uuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a {@link PlayerGetter} that resolves the player by username
     * from the BungeeCord proxy.
     */
    @Override
    public PlayerGetter<ProxiedPlayer> getPlayerGetter(String playerName) {
        return () -> ProxyServer.getInstance().getPlayer(playerName);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a {@link RealPlayer} backed by {@code playerGetter} that
     * provides BungeeCord-specific implementations for chat, command dispatch,
     * messaging, and permission management.
     */
    @Override
    public RealPlayer<ProxiedPlayer> getPlayer(PlayerGetter<ProxiedPlayer> playerGetter) {
        return new RealPlayer<>(playerGetter) {
            /**
             * {@inheritDoc}
             */
            @Override
            public void chatAs(String command) {
                getPlayer().chat(command);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void runCommand(String command) {
                ProxyServer.getInstance().getPluginManager().dispatchCommand(getPlayer(), command);
            }

            /**
             * {@inheritDoc}
             *
             * <p>Applies colour processing via {@link Messenger#codedText}.
             */
            @Override
            public void sendMessage(String message) {
                getPlayer().sendMessage(Messenger.getInstance().codedText(message));
            }

            /**
             * {@inheritDoc}
             *
             * <p>Sends a plain {@link net.md_5.bungee.api.chat.TextComponent}
             * without additional colour processing.
             */
            @Override
            public void sendMessageRaw(String message) {
                getPlayer().sendMessage(new TextComponent(message));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean hasPermission(String permission) {
                return getPlayer().hasPermission(permission);
            }

            /**
             * {@inheritDoc}
             *
             * <p>Delegates to {@link LuckPermsHandler#addPermission}.
             */
            @Override
            public void addPermission(String permission) {
                LuckPermsHandler.addPermission(getPlayer().getUniqueId().toString(), permission);
            }

            /**
             * {@inheritDoc}
             *
             * <p>Delegates to {@link LuckPermsHandler#removePermission}.
             */
            @Override
            public void removePermission(String permission) {
                LuckPermsHandler.removePermission(getPlayer().getUniqueId().toString(), permission);
            }
        };
    }
}
