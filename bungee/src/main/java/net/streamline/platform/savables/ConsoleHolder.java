package net.streamline.platform.savables;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.streamline.platform.Messenger;
import singularity.interfaces.audiences.IConsoleHolder;
import singularity.interfaces.audiences.real.RealSender;

/**
 * BungeeCord implementation of {@link IConsoleHolder} that wraps the proxy
 * console {@link CommandSender} inside a {@link RealSender}.
 *
 * <p>Provides permission checks, message sending (with colour processing),
 * raw message sending, log-level output, and command dispatch for the console.
 */
@Getter @Setter
public class ConsoleHolder implements IConsoleHolder<CommandSender> {

    /**
     * The wrapped console sender; delegates to {@link ProxyServer#getConsole()}
     * via the supplier passed to {@link RealSender}.
     */
    private RealSender<CommandSender> realConsole;

    /**
     * Constructs the {@code ConsoleHolder} and initialises the {@link RealSender}
     * backed by the BungeeCord proxy console.
     */
    public ConsoleHolder() {
        this.realConsole = new RealSender<>(() -> ProxyServer.getInstance().getConsole()) {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean hasPermission(String permission) {
                return getConsole().hasPermission(permission);
            }

            /**
             * {@inheritDoc}
             *
             * <p>Permission addition is not supported for the BungeeCord console
             * and is intentionally a no-op.
             */
            @Override
            public void addPermission(String permission) {
                // getConsole().addPermission(permission);
            }

            /**
             * {@inheritDoc}
             *
             * <p>Permission removal is not supported for the BungeeCord console
             * and is intentionally a no-op.
             */
            @Override
            public void removePermission(String permission) {
                // getConsole().removePermission(permission);
            }

            /**
             * {@inheritDoc}
             *
             * <p>Applies colour processing via {@link Messenger#codedText} before
             * delivering the message to the proxy console.
             */
            @Override
            public void sendMessage(String message) {
                getConsole().sendMessage(Messenger.getInstance().codedText(message));
            }

            /**
             * {@inheritDoc}
             *
             * <p>Wraps the raw string in a {@link TextComponent} and sends it to
             * the proxy console without colour processing.
             */
            @Override
            public void sendMessageRaw(String message) {
                getConsole().sendMessage(new TextComponent(message));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void sendConsoleMessageNonNull(String message) {
                sendMessage(message);
            }

            /**
             * {@inheritDoc}
             *
             * <p>Logs at {@code INFO} level via the BungeeCord proxy logger.
             */
            @Override
            public void sendLogMessage(String message) {
                ProxyServer.getInstance().getLogger().info(message);
            }

            /**
             * {@inheritDoc}
             *
             * <p>Dispatches the command through the BungeeCord plugin manager
             * as the console sender.
             */
            @Override
            public void runCommand(String command) {
                ProxyServer.getInstance().getPluginManager().dispatchCommand(getConsole(), command);
            }
        };
    }
}
