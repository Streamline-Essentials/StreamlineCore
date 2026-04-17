package net.streamline.platform.savables;

import host.plas.bou.commands.Sender;
import lombok.Getter;
import lombok.Setter;
import net.streamline.base.StreamlineSpigot;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import singularity.interfaces.audiences.IConsoleHolder;
import singularity.interfaces.audiences.real.RealSender;

/**
 * Spigot implementation of {@link IConsoleHolder} that wraps the Bukkit console
 * {@link CommandSender} in a cross-platform {@link RealSender} abstraction.
 *
 * <p>This class provides the console's permission checks, message dispatch, and
 * command execution via the Bukkit {@link Sender} utility, routing logging through
 * {@link StreamlineSpigot}.
 */
@Getter @Setter
public class ConsoleHolder implements IConsoleHolder<CommandSender> {
    /**
     * The cross-platform wrapper around the Bukkit console {@link CommandSender}.
     * Provides permission checks, messaging, and command execution for the server console.
     */
    private RealSender<CommandSender> realConsole;

    /**
     * Constructs a new {@code ConsoleHolder} and initialises the underlying
     * {@link RealSender} backed by {@link Bukkit#getConsoleSender()}.
     */
    public ConsoleHolder() {
        this.realConsole = new RealSender<>(Bukkit::getConsoleSender) {
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
                new Sender(getConsole()).sendMessage(message);
            }

            @Override
            public void sendMessageRaw(String message) {
                new Sender(getConsole()).sendMessage(message, false);
            }

            @Override
            public void sendConsoleMessageNonNull(String message) {
                sendMessage(message);
            }

            @Override
            public void sendLogMessage(String message) {
                StreamlineSpigot.getInstance().logInfo(message);
            }

            @Override
            public void runCommand(String command) {
                new Sender(getConsole()).executeCommand(command);
            }
        };
    }
}
