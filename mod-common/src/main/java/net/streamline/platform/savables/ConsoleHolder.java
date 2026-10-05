package net.streamline.platform.savables;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.MinecraftServer;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;
import singularity.interfaces.audiences.IConsoleHolder;
import singularity.interfaces.audiences.real.RealSender;

@Getter
@Setter
public class ConsoleHolder implements IConsoleHolder<Object> {

    private RealSender<Object> realConsole;

    public ConsoleHolder() {
        this.realConsole = new RealSender<Object>(() -> {
            MinecraftServer server = BasePlugin.getServer();
            return server != null ? server.createCommandSourceStack() : null;
        }) {
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
                BasePlugin.getInstance().getSlf4jLogger().info(message);
            }

            @Override
            public void runCommand(String command) {
                MinecraftServer server = BasePlugin.getServer();
                if (server == null) return;
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            }
        };
    }
}
