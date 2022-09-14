package net.streamline.api.interfaces;

import net.luckperms.api.LuckPerms;
import net.streamline.api.command.ModuleCommand;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.holders.GeyserHolder;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.placeholder.RATAPI;
import net.streamline.api.profile.StreamlineProfiler;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    @NotNull Collection<StreamlinePlayer> getOnlinePlayers();

    List<String> getOnlinePlayerNames();

    boolean hasPermission(StreamlineUser user, String permission);

    List<String> getServerNames();

    void chatAs(StreamlineUser as, String message);

    void runAsStrictly(StreamlineUser as, String message);

    boolean serverHasPlugin(String plugin);

    StreamlineServerInfo getStreamlineServer(String server);

    void setStreamlineServer(StreamlineServerInfo server);

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
}
