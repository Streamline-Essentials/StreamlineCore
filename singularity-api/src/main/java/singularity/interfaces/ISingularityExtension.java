package singularity.interfaces;

import singularity.command.CosmicCommand;
import singularity.data.players.CosmicPlayer;
import singularity.events.CosmicEvent;
import singularity.objects.CosmicResourcePack;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentSkipListSet;

public interface ISingularityExtension {
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

    void fireEvent(CosmicEvent event);

    void fireEvent(CosmicEvent event, boolean async);

    void handleMisSync(CosmicEvent event, boolean async);

    @NotNull ConcurrentSkipListSet<CosmicPlayer> getOnlinePlayers();

    ConcurrentSkipListSet<String> getOnlinePlayerNames();

    ConcurrentSkipListSet<String> getServerNames();

    boolean serverHasPlugin(String plugin);

    boolean equalsAnyServer(String servername);

    IProperCommand createCommand(CosmicCommand command);

    int getMaxPlayers();

    long getConnectionThrottle();

    boolean getOnlineMode();

    void shutdown();

    int broadcast(@NotNull String message, @NotNull String permission);

    String getVersion();

    void sendResourcePack(CosmicResourcePack resourcePack, CosmicPlayer player);

    void sendResourcePack(CosmicResourcePack resourcePack, String uuid);

    ClassLoader getMainClassLoader();

    String getName();
}
