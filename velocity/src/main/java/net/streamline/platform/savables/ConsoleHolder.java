package net.streamline.platform.savables;

import com.velocitypowered.api.command.CommandSource;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.streamline.base.StreamlineVelocity;
import net.streamline.platform.Messenger;
import singularity.interfaces.audiences.IConsoleHolder;
import singularity.interfaces.audiences.real.RealSender;

/**
 * Velocity implementation of {@link IConsoleHolder} that wraps the Velocity console
 * {@link CommandSource} in a cross-platform {@link RealSender} abstraction.
 *
 * <p>Messages are dispatched via the Adventure {@link Component} API through
 * {@link Messenger#codedText(String)}, and command execution is delegated to
 * {@link com.velocitypowered.api.command.CommandManager#executeAsync}.
 */
@Getter @Setter
public class ConsoleHolder implements IConsoleHolder<CommandSource> {
    /**
     * The cross-platform wrapper around the Velocity console {@link CommandSource}.
     * Provides permission checks, messaging, logging, and command execution for the proxy console.
     */
    private RealSender<CommandSource> realConsole;

    /**
     * Constructs a new {@code ConsoleHolder} and initialises the underlying
     * {@link RealSender} backed by the Velocity proxy console command source.
     */
    public ConsoleHolder() {
        this.realConsole = new RealSender<>(StreamlineVelocity.getInstance().getProxy()::getConsoleCommandSource) {
            @Override
            public boolean hasPermission(String permission) {
                return getConsole().hasPermission(permission);
            }

            @Override
            public void addPermission(String permission) {
                // getConsole().addPermission(permission);
            }

            @Override
            public void removePermission(String permission) {
                // getConsole().removePermission(permission);
            }

            @Override
            public void sendMessage(String message) {
                getConsole().sendMessage(Messenger.getInstance().codedText(message));
            }

            @Override
            public void sendMessageRaw(String message) {
                getConsole().sendMessage(Component.text(message));
            }

            @Override
            public void sendConsoleMessageNonNull(String message) {
                sendMessage(message);
            }

            @Override
            public void sendLogMessage(String message) {
                StreamlineVelocity.getInstance().getLogger().info(message);
            }

            @Override
            public void runCommand(String command) {
                StreamlineVelocity.getInstance().getProxy().getCommandManager().executeAsync(getConsole(), command);
            }
        };
    }
}
