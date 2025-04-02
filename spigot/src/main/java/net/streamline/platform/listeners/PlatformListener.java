package net.streamline.platform.listeners;

import host.plas.bou.utils.ClassHelper;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.base.Streamline;
import net.streamline.base.TenSecondTimer;
import net.streamline.platform.Messenger;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.savables.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.NotNull;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;
import singularity.configs.given.whitelist.WhitelistConfig;
import singularity.configs.given.whitelist.WhitelistEntry;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.uuid.UuidManager;
import singularity.events.player.location.PlayerMovementEvent;
import singularity.events.server.*;
import singularity.events.server.ping.PingReceivedEvent;
import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleManager;
import singularity.modules.ModuleUtils;
import singularity.objects.PingedResponse;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;
import tv.quaint.events.BaseEventHandler;
import tv.quaint.events.BaseEventListener;
import tv.quaint.events.processing.BaseProcessor;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PlatformListener implements Listener {
    @Getter @Setter
    private static boolean messaged = false;
    @Getter @Setter
    private static boolean joined = false;
    @Getter @Setter
    private static BaseProcessorListener processorListener;

    public static boolean isTested() {
        return isMessaged() && isJoined();
    }

    @Getter @Setter
    public static PaperListener paperListener;

    public PlatformListener() {
        MessageUtils.logInfo("BaseListener registered!");
        setProcessorListener(new BaseProcessorListener());
        ModuleUtils.listen(getProcessorListener(), SLAPI.getBaseModule());

        if (ClassHelper.isPaper()) {
            paperListener = new PaperListener();
        }
    }

    @EventHandler
    public void onPreJoin(AsyncPlayerPreLoginEvent event) {
        String uuid = event.getUniqueId().toString();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(uuid);

        WhitelistConfig whitelistConfig = GivenConfigs.getWhitelistConfig();
        if (whitelistConfig.isEnabled()) {
            WhitelistEntry entry = whitelistConfig.getEntry(streamPlayer.getUuid());
            if (entry == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, MessageUtils.codedString(MainMessagesHandler.MESSAGES.INVALID.WHITELIST_NOT.get()));
                return;
            }
        }

        LoginReceivedEvent loginReceivedEvent = new LoginReceivedEvent(streamPlayer);
        BaseEventHandler.fireEvent(loginReceivedEvent);

        if (loginReceivedEvent.getResult().isCancelled()) {
            if (! loginReceivedEvent.getResult().validate()) return;

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, MessageUtils.codedString(loginReceivedEvent.getResult().getDisconnectMessage()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        UuidManager.cachePlayer(player.getUniqueId().toString(), player.getName(), UserManager.getInstance().parsePlayerIP(player));

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        streamPlayer.setCurrentIp(UserManager.getInstance().parsePlayerIP(player));
        streamPlayer.setCurrentName(player.getName());

        CosmicLocation location = streamPlayer.getLocation();

        Location loc = player.getLocation();
        World w = loc.getWorld();
        if (w != null) {
            PlayerWorld world = new PlayerWorld(w.getName());
            WorldPosition position = new WorldPosition(loc.getX(), loc.getY(), loc.getZ());
            PlayerRotation rotation = new PlayerRotation(loc.getYaw(), loc.getPitch());

            CosmicLocation newLocation = new CosmicLocation(location.getServer(), world, position, rotation);

//            PlayerMovementEvent e = new PlayerMovementEvent(streamPlayer, newLocation).fire();
//            e.completeMovement();

            streamPlayer.setLocation(newLocation);
            streamPlayer.save();
        }

        LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(streamPlayer);
        ModuleUtils.fireEvent(loginCompletedEvent);

        setJoined(true);

        new TenSecondTimer(player);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        UuidManager.cachePlayer(player.getUniqueId().toString(), player.getName(), UserManager.getInstance().parsePlayerIP(player.getUniqueId().toString()));

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        LogoutEvent logoutEvent = new LogoutEvent(streamPlayer);
        ModuleUtils.fireEvent(logoutEvent);

        streamPlayer.save();
        UserUtils.unloadSender(streamPlayer);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        CosmicChatEvent chatEvent = new CosmicChatEvent(streamPlayer, event.getMessage());
        Streamline.getInstance().fireEvent(chatEvent, true);
        if (chatEvent.isCanceled()) {
            event.setCancelled(true);
            return;
        }
        event.setMessage(chatEvent.getMessage());
    }

    @EventHandler
    public void onProperEvent(ProperEvent event) {
        ModuleManager.fireEvent(event.getCosmicEvent());
    }

    public static class ProxyMessagingListener implements PluginMessageListener {
        public ProxyMessagingListener() {
            MessageUtils.logInfo("Registered " + getClass().getSimpleName() + "!");
        }

        @Override
        public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
            CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

            try {
                ProxiedMessage messageIn = new ProxiedMessage(streamPlayer, true, message, channel);
                ProxyMessageInEvent e = new ProxyMessageInEvent(messageIn).fire();
                if (e.isCancelled()) return;
                SLAPI.getInstance().getProxyMessenger().receiveMessage(e);
            } catch (Exception e) {
                // do nothing.
            }
        }
    }

    @EventHandler
    public void onStart(ServerLoadEvent event) {
        ServerStartEvent e = new ServerStartEvent().fire();
        if (e.isCancelled()) return;
        if (! e.isSendable()) return;
        SLAPI.sendConsoleMessage(e.getMessage());
    }

    public static class BaseProcessorListener implements BaseEventListener {
        public BaseProcessorListener() {
            MessageUtils.logInfo("Registered " + getClass().getSimpleName() + "!");
        }

        @BaseProcessor
        public void onProxiedMessageReceived(ProxyMessageInEvent event) {
            setMessaged(true);
        }
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        if (ClassHelper.isPaper()) return; // Handled by PaperListener

        String hostName;
        try {
            hostName = event.getAddress().getHostName();
        } catch (Throwable e) {
            hostName = "";
        }

        PingedResponse.Protocol protocol = new PingedResponse.Protocol("latest", 1);

        List<PingedResponse.PlayerInfo> playerInfos = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerInfos.add(new PingedResponse.PlayerInfo(player.getName(), player.getUniqueId().toString()));
        }

        PingedResponse.Players players = new PingedResponse.Players(event.getMaxPlayers(), event.getNumPlayers(),
                playerInfos.toArray(new PingedResponse.PlayerInfo[0]));

        PingedResponse response;
        try {
            response = new PingedResponse(protocol, players, event.getMotd());
        } catch (Throwable e) {
            Streamline.getInstance().logWarning("Failed to create PingedResponse: " + e.getMessage());
            Streamline.getInstance().logWarning(e.getStackTrace());
            return;
        }

        PingReceivedEvent pingReceivedEvent = new PingReceivedEvent(response, hostName).fire();

        if (pingReceivedEvent.isCancelled()) {
            return;
        }

        event.setMotd(Messenger.colorAsString(pingReceivedEvent.getResponse().getDescription()));

        // Set the sample of the server (the players displayed when hovering over the player count)
        // does not work right now...

        event.setMaxPlayers(pingReceivedEvent.getResponse().getPlayers().getMax());
//        event.setNumPlayers(pingReceivedEvent.getResponse().getPlayers().getOnline());

        try {
            CachedServerIcon icon = Bukkit.loadServerIcon(Paths.get(pingReceivedEvent.getResponse().getFaviconString()).toFile());
            event.setServerIcon(icon);
        } catch (Exception e) {
            // do nothing.
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());
        if (streamPlayer == null) return;

        Location loc = event.getTo();
        if (loc == null) return;
        if (loc.getWorld() == null) return;

        CosmicLocation location = streamPlayer.getLocation();
        PlayerWorld world = new PlayerWorld(loc.getWorld().getName());
        WorldPosition position = new WorldPosition(loc.getX(), loc.getY(), loc.getZ());
        PlayerRotation rotation = new PlayerRotation(loc.getYaw(), loc.getPitch());

        CosmicLocation newLocation = new CosmicLocation(location.getServer(), world, position, rotation);

        PlayerMovementEvent e = new PlayerMovementEvent(streamPlayer, newLocation).fire();
        if (e.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        streamPlayer.setLocation(e.getNewLocation());
    }
}
