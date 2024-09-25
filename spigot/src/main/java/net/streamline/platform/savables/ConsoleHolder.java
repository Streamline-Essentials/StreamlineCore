package net.streamline.platform.savables;

import lombok.Getter;
import lombok.Setter;
import net.streamline.platform.Messenger;
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
                getConsole().sendMessage(Messenger.getInstance().codedString(message));
            }

            @Override
            public void sendMessageRaw(String message) {
                getConsole().sendMessage(message);
            }

            @Override
            public void sendConsoleMessageNonNull(String message) {
                sendMessage(message);
            }

            @Override
            public void sendLogMessage(String message) {
                Bukkit.getLogger().info(message);
            }

            @Override
            public void runCommand(String command) {
                Bukkit.dispatchCommand(getConsole(), command);
            }
        };
    }
}
