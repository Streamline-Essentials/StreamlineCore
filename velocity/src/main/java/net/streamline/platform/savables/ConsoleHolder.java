package net.streamline.platform.savables;

import com.velocitypowered.api.command.CommandSource;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.streamline.api.interfaces.audiences.IConsoleHolder;
import net.streamline.api.interfaces.audiences.real.RealSender;
import net.streamline.base.StreamlineVelocity;
import net.streamline.platform.Messenger;

@Getter @Setter
public class ConsoleHolder implements IConsoleHolder<CommandSource> {
    private RealSender<CommandSource> realConsole;

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
