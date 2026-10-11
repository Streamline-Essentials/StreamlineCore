package net.streamline.platform.savables;

import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;
import singularity.interfaces.audiences.IConsoleHolder;
import singularity.interfaces.audiences.real.RealSender;

/**
 * The console of a 1.8.9 client: the game's log. It has no command line to run commands on.
 */
public class ConsoleHolder implements IConsoleHolder<Object> {

    private final RealSender<Object> realConsole;

    public ConsoleHolder() {
        this.realConsole = new RealSender<Object>(() -> null) {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }

            @Override
            public void addPermission(String permission) {
                // The console holds every permission already.
            }

            @Override
            public void removePermission(String permission) {
                // The console holds every permission already.
            }

            @Override
            public void sendMessage(String message) {
                Messenger messenger = Messenger.getInstance();
                sendLogMessage(messenger != null ? messenger.stripColor(message) : message);
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
                BasePlugin.log(message);
            }

            @Override
            public void runCommand(String command) {
            }
        };
    }

    @Override
    public RealSender<Object> getRealConsole() {
        return realConsole;
    }
}
