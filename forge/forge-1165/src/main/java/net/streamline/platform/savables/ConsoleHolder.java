package net.streamline.platform.savables;

import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;
import singularity.interfaces.audiences.IConsoleHolder;
import singularity.interfaces.audiences.real.RealSender;

@Getter
@Setter
@SuppressWarnings("unchecked")
public class ConsoleHolder implements IConsoleHolder<Object> {

    private RealSender<Object> realConsole;

    public ConsoleHolder() {
        this.realConsole = new RealSender<Object>(() ->
                ServerLifecycleHooks.getCurrentServer() != null
                        ? ServerLifecycleHooks.getCurrentServer().createCommandSourceStack()
                        : null
        ) {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }

            @Override
            public void addPermission(String permission) {
                // no-op for console
            }

            @Override
            public void removePermission(String permission) {
                // no-op for console
            }

            @Override
            public void sendMessage(String message) {
                String colored = Messenger.getInstance() != null
                        ? Messenger.getInstance().codedString(message) : message;
                sendLogMessage(colored);
            }

            @Override
            public void sendMessageRaw(String message) {
                sendLogMessage(message);
            }

            @Override
            public void sendConsoleMessageNonNull(String message) {
                sendMessage(message);
            }

            @Override
            public void sendLogMessage(String message) {
                BasePlugin.getInstance().getSlf4jLogger().info(message);
            }

            @Override
            public void runCommand(String command) {
                if (ServerLifecycleHooks.getCurrentServer() == null) return;
                ServerLifecycleHooks.getCurrentServer()
                        .getCommands()
                        .performCommand(
                                ServerLifecycleHooks.getCurrentServer().createCommandSourceStack(),
                                command);
            }
        };
    }
}
