package net.streamline.platform.savables;

import host.plas.bou.commands.Sender;
import lombok.Getter;
import lombok.Setter;
import net.streamline.base.Streamline;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import singularity.interfaces.audiences.IConsoleHolder;
import singularity.interfaces.audiences.real.RealSender;

@Getter @Setter
public class ConsoleHolder implements IConsoleHolder<CommandSender> {
    private RealSender<CommandSender> realConsole;

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
                Streamline.getInstance().logInfo(message);
            }

            @Override
            public void runCommand(String command) {
                new Sender(getConsole()).executeCommand(command);
            }
        };
    }
}
