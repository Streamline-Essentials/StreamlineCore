package net.streamline.api.interfaces;

import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentSkipListSet;

public interface IStreamline {
    enum PlatformType {
        BUNGEE,
        SPIGOT,
        VELOCITY,
        ;
    }
    enum ServerType {
        PROXY,
        BACKEND
        ;
    }

    PlatformType getPlatformType();

    ServerType getServerType();

    void fireEvent(IProperEvent<?> event);

    void fireEvent(StreamlineEvent event);

    void fireEvent(StreamlineEvent event, boolean async);

    void handleMisSync(StreamlineEvent event, boolean async);

    @NotNull ConcurrentSkipListSet<StreamlinePlayer> getOnlinePlayers();

    ConcurrentSkipListSet<String> getOnlinePlayerNames();

    boolean hasPermission(StreamlineUser user, String permission);

    ConcurrentSkipListSet<String> getServerNames();

    void chatAs(StreamlineUser as, String message);

    void runAsStrictly(StreamlineUser as, String message);

    boolean serverHasPlugin(String plugin);

    boolean equalsAnyServer(String servername);

    IProperCommand createCommand(StreamlineCommand command);

    int getMaxPlayers();

    long getConnectionThrottle();

    boolean getOnlineMode();

    void shutdown();

    int broadcast(@NotNull String message, @NotNull String permission);

    String getVersion();

    void sendResourcePack(StreamlineResourcePack resourcePack, StreamlineUser player);

    void sendResourcePack(StreamlineResourcePack resourcePack, String uuid);

    ClassLoader getMainClassLoader();

    String getName();
}
