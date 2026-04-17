package net.streamline.platform.listeners;

import gg.drak.thebase.async.AsyncUtils;
import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.bou.utils.ClassHelper;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.base.StreamlineSpigot;
import net.streamline.base.TenSecondTimer;
import net.streamline.platform.Messenger;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.savables.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
import singularity.data.server.CosmicServer;
import singularity.data.teleportation.TPTicket;
import singularity.data.uuid.UuidManager;
import singularity.events.player.location.PlayerMovementEvent;
import singularity.events.server.*;
import singularity.events.server.ping.PingReceivedEvent;
import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleManager;
import singularity.modules.ModuleUtils;
import singularity.objects.PingedResponse;
import singularity.objects.world.CosmicBlock;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Core Bukkit event listener for the StreamlineCore Spigot platform.
 *
 * <p>Handles the fundamental player lifecycle events (pre-login, join, quit,
 * chat), server lifecycle events (start, ping), and world interaction events
 * (block break/place, player movement) by translating them into their
 * cross-platform {@link singularity.events.CosmicEvent} equivalents and
 * firing them via the Singularity event bus.
 *
 * <p>On construction this listener also conditionally registers a
 * {@link PaperListener} when running on a Paper server, and sets up the
 * {@link BaseProcessorListener} for internal proxy-message tracking.
 */
public class PlatformListener implements Listener {
    /**
     * {@code true} once the first proxied plugin message has been received,
     * used to track whether cross-proxy communication is functioning.
     */
    @Getter @Setter
    private static boolean messaged = false;

    /**
     * {@code true} once the first player has successfully completed the join
     * sequence, used to track platform readiness.
     */
    @Getter @Setter
    private static boolean joined = false;

    /**
     * The TheBase event listener responsible for processing
     * {@link singularity.messages.events.ProxyMessageInEvent} callbacks.
     */
    @Getter @Setter
    private static BaseProcessorListener processorListener;

    /**
     * Returns {@code true} when both {@link #isMessaged()} and
     * {@link #isJoined()} are {@code true}, indicating the platform has fully
     * initialised and proven two-way proxy communication.
     *
     * @return {@code true} if the platform has passed basic connectivity tests
     */
    public static boolean isTested() {
        return isMessaged() && isJoined();
    }

    /**
     * The Paper-specific listener instance, or {@code null} when not running
     * on a Paper server.
     */
    @Getter @Setter
    public static PaperListener paperListener;

    /**
     * Constructs and initialises the platform listener, registers the
     * {@link BaseProcessorListener} with the Singularity event bus, and
     * conditionally registers the {@link PaperListener}.
     */
    public PlatformListener() {
        MessageUtils.logInfo("BaseListener registered!");
        setProcessorListener(new BaseProcessorListener());
        ModuleUtils.listen(getProcessorListener(), SLAPI.getBaseModule());

        if (ClassHelper.isPaper()) {
            paperListener = new PaperListener();
        }
    }

    /**
     * Handles the asynchronous pre-login event, enforcing the whitelist if
     * enabled and firing a {@link singularity.events.server.LoginReceivedEvent}.
     * Disconnects the player if the event result is cancelled.
     *
     * @param event the async pre-login event
     */
    @EventHandler
    public void onPreJoin(AsyncPlayerPreLoginEvent event) {
        String uuid = event.getUniqueId().toString();

        WhitelistConfig whitelistConfig = GivenConfigs.getWhitelistConfig();
        if (whitelistConfig.isEnabled()) {
            WhitelistEntry entry = whitelistConfig.getEntry(uuid);
            if (entry == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, MessageUtils.codedString(MainMessagesHandler.MESSAGES.INVALID.WHITELIST_NOT.get()));
                return;
            }
        }

        CosmicPlayer streamPlayer = UserUtils.getOrGetPlayer(uuid).orElse(null);
        if (streamPlayer == null) return;

        LoginReceivedEvent loginReceivedEvent = new LoginReceivedEvent(streamPlayer);
        BaseEventHandler.fireEvent(loginReceivedEvent);

        if (loginReceivedEvent.getResult().isCancelled()) {
            if (! loginReceivedEvent.getResult().validate()) return;

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, MessageUtils.codedString(loginReceivedEvent.getResult().getDisconnectMessage()));
        }
    }

    /**
     * Handles a player join, asynchronously creating or loading the player's
     * {@link singularity.data.players.CosmicPlayer}, syncing location, firing a
     * {@link singularity.events.server.LoginCompletedEvent}, starting the
     * {@link net.streamline.base.TenSecondTimer}, and applying any pending
     * teleport tickets.
     *
     * @param event the player join event
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        UuidManager.cachePlayer(player.getUniqueId().toString(), player.getName(), UserManager.getInstance().parsePlayerIP(player));

        AsyncUtils.executeAsync(() -> {
            CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
            if (streamPlayer == null) {
                MessageUtils.logWarning("Failed to create CosmicPlayer for " + player.getName() + " (" + player.getUniqueId() + ")");
                return;
            }

            streamPlayer.setCurrentIp(UserManager.getInstance().parsePlayerIP(player));
            streamPlayer.setCurrentName(player.getName());

            CosmicServer server = SLAPI.getServer().getCosmicServer();

            Location loc = player.getLocation();
            World w = loc.getWorld();
            if (w != null) {
                PlayerWorld world = new PlayerWorld(w.getName());
                WorldPosition position = new WorldPosition(loc.getX(), loc.getY(), loc.getZ());
                PlayerRotation rotation = new PlayerRotation(loc.getYaw(), loc.getPitch());

                CosmicLocation newLocation = new CosmicLocation(server, world, position, rotation);

//            PlayerMovementEvent e = new PlayerMovementEvent(streamPlayer, newLocation).fire();
//            e.completeMovement();

                streamPlayer.setLocation(newLocation);
                streamPlayer.save();
            }

            LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(streamPlayer);
            ModuleUtils.fireEvent(loginCompletedEvent);

            setJoined(true);

            new TenSecondTimer(player);

            TPTicket ticket = TPTicket.get(player.getUniqueId().toString());
            if (ticket != null) {
                ticket.teleportWithDelayAndClear(20);
                ticket.clear();
            }
        });
    }

    /**
     * Handles a player quit, saving the player's data and firing a
     * {@link singularity.events.server.LogoutEvent} before unloading the
     * {@link singularity.data.players.CosmicPlayer} from memory.
     *
     * @param event the player quit event
     */
    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        UuidManager.cachePlayer(player.getUniqueId().toString(), player.getName(), UserManager.getInstance().parsePlayerIP(player.getUniqueId().toString()));

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
        if (streamPlayer == null) {
            MessageUtils.logWarning("Failed to create CosmicPlayer for " + player.getName() + " (" + player.getUniqueId() + ")");
            return;
        }

        LogoutEvent logoutEvent = new LogoutEvent(streamPlayer);
        ModuleUtils.fireEvent(logoutEvent);

        streamPlayer.save();
        UserUtils.unloadSender(streamPlayer);
    }

    /**
     * Handles an async chat event by firing a cross-platform
     * {@link singularity.events.server.CosmicChatEvent}. If the event is
     * cancelled, suppresses the Bukkit message; otherwise propagates any
     * modified message text back onto the Bukkit event.
     *
     * @param event the async player chat event
     */
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
        if (streamPlayer == null) {
            MessageUtils.logWarning("Failed to create CosmicPlayer for " + player.getName() + " (" + player.getUniqueId() + ")");
            return;
        }

        CosmicChatEvent chatEvent = new CosmicChatEvent(streamPlayer, event.getMessage());
        StreamlineSpigot.getInstance().fireEvent(chatEvent, true);
        if (chatEvent.isCanceled()) {
            event.setCancelled(true);
            return;
        }
        event.setMessage(chatEvent.getMessage());
    }

    /**
     * Forwards a {@link ProperEvent} (which wraps a cross-platform
     * {@link singularity.events.CosmicEvent}) to the module manager so that
     * all loaded modules may process it.
     *
     * @param event the wrapped cosmic event
     */
    @EventHandler
    public void onProperEvent(ProperEvent event) {
        ModuleManager.fireEvent(event.getCosmicEvent());
    }

    /**
     * Plugin-messaging listener that receives inbound messages on the Streamline
     * API channel and dispatches them as
     * {@link singularity.messages.events.ProxyMessageInEvent} events.
     */
    public static class ProxyMessagingListener implements PluginMessageListener {
        /**
         * Constructs and logs the registration of this listener.
         */
        public ProxyMessagingListener() {
            MessageUtils.logInfo("Registered " + getClass().getSimpleName() + "!");
        }

        /**
         * {@inheritDoc}
         *
         * <p>Wraps the raw byte array in a {@link singularity.messages.proxied.ProxiedMessage},
         * fires a {@link singularity.messages.events.ProxyMessageInEvent}, and if
         * not cancelled forwards it to the configured proxy messenger.
         */
        @Override
        public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
            CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
            if (streamPlayer == null) {
                MessageUtils.logWarning("Failed to create CosmicPlayer for " + player.getName() + " (" + player.getUniqueId() + ")");
                return;
            }

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

    /**
     * Handles the server load event, firing a cross-platform
     * {@link singularity.events.server.ServerStartEvent} and broadcasting its
     * message if not cancelled and sendable.
     *
     * @param event the server load event
     */
    @EventHandler
    public void onStart(ServerLoadEvent event) {
        ServerStartEvent e = new ServerStartEvent().fire();
        if (e.isCancelled()) return;
        if (! e.isSendable()) return;
        SLAPI.sendConsoleMessage(e.getMessage());
    }

    /**
     * TheBase {@link BaseEventListener} that tracks whether a proxied plugin
     * message has been successfully received, updating {@link #messaged}.
     */
    public static class BaseProcessorListener implements BaseEventListener {
        /**
         * Constructs and logs the registration of this listener.
         */
        public BaseProcessorListener() {
            MessageUtils.logInfo("Registered " + getClass().getSimpleName() + "!");
        }

        /**
         * Marks the {@link PlatformListener#messaged} flag as {@code true}
         * upon receiving the first successful proxy message, confirming that
         * bidirectional plugin-channel communication is operational.
         *
         * @param event the inbound proxy message event
         */
        @BaseProcessor
        public void onProxiedMessageReceived(ProxyMessageInEvent event) {
            setMessaged(true);
        }
    }

    /**
     * Handles the vanilla Spigot server-list ping event (used only when the
     * server is not running Paper, since Paper uses {@link PaperListener}
     * instead). Fires a cross-platform
     * {@link singularity.events.server.ping.PingReceivedEvent} and applies any
     * changes (MOTD, max players, server icon) to the outgoing response.
     *
     * @param event the server list ping event
     */
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
            StreamlineSpigot.getInstance().logWarning("Failed to create PingedResponse: " + e.getMessage());
            StreamlineSpigot.getInstance().logWarning(e.getStackTrace());
            return;
        }

        PingReceivedEvent pingReceivedEvent = new PingReceivedEvent(response, hostName).fire();

        if (pingReceivedEvent.isCancelled()) {
            return;
        }

        event.setMotd(Messenger.getInstance().codedString(pingReceivedEvent.getResponse().getDescription()));

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

    /**
     * Handles player movement, converting the destination Bukkit
     * {@link org.bukkit.Location} to a
     * {@link singularity.data.players.location.CosmicLocation} and firing a
     * cross-platform {@link singularity.events.player.location.PlayerMovementEvent}.
     * Cancels the Bukkit move event if the cosmic event is cancelled.
     *
     * @param event the player move event
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
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

    /**
     * Handles a block break, firing a cross-platform
     * {@link singularity.events.server.world.BlockBreakEvent}. Cancels the Bukkit
     * event if the cosmic event is cancelled.
     *
     * @param event the Bukkit block break event
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
        if (streamPlayer == null) {
            MessageUtils.logWarning("Failed to create CosmicPlayer for " + player.getName() + " (" + player.getUniqueId() + ")");
            return;
        }
        PlayerWorld world = new PlayerWorld(player.getWorld().getName());
        WorldPosition location = new WorldPosition(block.getX(), block.getY(), block.getZ());
        CosmicBlock b = new CosmicBlock(world, location, block.getType().toString());

        singularity.events.server.world.BlockBreakEvent e = new singularity.events.server.world.BlockBreakEvent(streamPlayer, b).fire();
        if (e.isCancelled()) {
            event.setCancelled(true);
            return;
        }
    }

    /**
     * Handles a block place, firing a cross-platform
     * {@link singularity.events.server.world.BlockPlaceEvent}. Cancels the Bukkit
     * event if the cosmic event is cancelled.
     *
     * @param event the Bukkit block place event
     */
    @EventHandler
    public void onBlockBreak(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
        if (streamPlayer == null) {
            MessageUtils.logWarning("Failed to create CosmicPlayer for " + player.getName() + " (" + player.getUniqueId() + ")");
            return;
        }
        PlayerWorld world = new PlayerWorld(player.getWorld().getName());
        WorldPosition location = new WorldPosition(block.getX(), block.getY(), block.getZ());
        CosmicBlock b = new CosmicBlock(world, location, block.getType().toString());

        singularity.events.server.world.BlockPlaceEvent e = new singularity.events.server.world.BlockPlaceEvent(streamPlayer, b).fire();
        if (e.isCancelled()) {
            event.setCancelled(true);
            return;
        }
    }
}
