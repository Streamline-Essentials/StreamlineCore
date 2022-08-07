package net.streamline.api.interfaces;

import net.luckperms.api.LuckPerms;
import net.streamline.api.command.ModuleCommand;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.configs.given.MainConfigHandler;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.holders.GeyserHolder;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.placeholder.RATAPI;
import net.streamline.api.profile.StreamlineProfiler;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.ModuleTaskManager;
import net.streamline.api.scheduler.TaskManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public interface IStreamline {
    void fireEvent(IProperEvent<?> event);

    void fireEvent(StreamlineEvent event);

    @NotNull Collection<StreamlinePlayer> getOnlinePlayers();

    List<String> getOnlinePlayerNames();

    boolean hasPermission(StreamlineUser user, String permission);

    LuckPerms getLuckPerms();

    RATAPI getRATAPI();

    String getUUIDFromName(String name);

    List<String> getServerNames();

    void chatAs(StreamlineUser as, String message);

    void runAsStrictly(StreamlineUser as, String message);

    boolean serverHasPlugin(String plugin);

    StreamlineServerInfo getStreamlineServer(String server);

    StreamlineProfiler getProfiler();

    boolean equalsAnyServer(String servername);

    void ensureApiChannel(String apiChannel);

    ModuleTaskManager getModuleScheduler();

    TaskManager getMainScheduler();

    int getMaxPlayers();

    StreamlineResourcePack getResourcePack();

    void setResourcePack(StreamlineResourcePack resourcePack);

    boolean hasWhitelist();

    void setWhitelist(boolean value);

    boolean isWhitelistEnforced();

    void setWhitelistEnforced(boolean value);

    @NotNull Set<StreamlinePlayer> getWhitelistedPlayers();

    void reloadWhitelist();

    int broadcastMessage(@NotNull String message);

    void flushCommands();

    void registerStreamlineCommand(StreamlineCommand command);

    void unregisterStreamlineCommand(StreamlineCommand command);

    void registerModuleCommand(ModuleCommand command);

    void unregisterModuleCommand(ModuleCommand command);

    MainConfigHandler getMainConfig();

    MainMessagesHandler getMainMessages();

    File getUserFolder();

    File getModuleFolder();

    File getMainCommandsFolder();

    String getCommandsFolderChild();

    @NotNull String getUpdateFolderPath();

    @NotNull File getUpdateFolder();

    long getConnectionThrottle();

    @Nullable StreamlinePlayer getSavedPlayer(@NotNull String name);

    @Nullable StreamlinePlayer getSavedPlayerByUUID(@NotNull String uuid);

    String getNameFromUUID(String uuid);

    ModuleCommand getModuleCommand(@NotNull String name);

    Map<String, String[]> getCommandAliases();

    boolean getOnlineMode();

    void shutdown();

    int broadcast(@NotNull String message, @NotNull String permission);

    @NotNull StreamlinePlayer getOfflinePlayer(@NotNull String name);

    @NotNull StreamlinePlayer getOfflinePlayer(@NotNull UUID id);

    @NotNull StreamlinePlayer createPlayerProfile(@Nullable UUID uniqueId, @Nullable String name);

    @NotNull StreamlinePlayer createPlayerProfile(@NotNull UUID uniqueId);

    @NotNull StreamlinePlayer createPlayerProfile(@NotNull String name);

    @NotNull Set<String> getIPBans();

    void banIP(@NotNull String address);

    void unbanIP(@NotNull String address);

    @NotNull Set<StreamlinePlayer> getBannedPlayers();

    @NotNull Set<StreamlinePlayer> getOperators();

    @NotNull StreamlinePlayer[] getOfflinePlayers();

    boolean isPrimaryThread();

    @NotNull String getMotd();

    @Nullable String getShutdownMessage();

    TreeMap<String, ModuleCommand> getLoadedModuleCommands();

    TreeMap<String, StreamlineCommand> getLoadedStreamlineCommands();

    GeyserHolder getGeyserHolder();

    String getVersion();
}
