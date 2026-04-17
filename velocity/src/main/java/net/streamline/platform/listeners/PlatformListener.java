package net.streamline.platform.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import gg.drak.thebase.async.AsyncUtils;
import net.streamline.api.SLAPI;
import net.streamline.base.StreamlineVelocity;
import net.streamline.platform.Messenger;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.savables.UserManager;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;
import singularity.configs.given.whitelist.WhitelistConfig;
import singularity.configs.given.whitelist.WhitelistEntry;
import singularity.data.players.CosmicPlayer;
import singularity.data.uuid.UuidManager;
import singularity.events.server.*;
import singularity.events.server.ping.PingReceivedEvent;
import singularity.messages.builders.ServerNameMessageBuilder;
import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleManager;
import singularity.modules.ModuleUtils;
import singularity.objects.CosmicFavicon;
import singularity.objects.PingedResponse;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Central Velocity event listener for StreamlineCore.
 *
 * <p>Handles all proxy-level events — player pre-login, post-login, disconnect, server switch,
 * chat, proxy ping, plugin messages, and kicked-from-server — and translates them into the
 * corresponding cross-platform {@link singularity.events.CosmicEvent} hierarchy. Whitelist
 * enforcement, UUID caching, {@link CosmicPlayer} lifecycle management, and bStats ping
 * modification all take place here.
 */
public class PlatformListener {
    /**
     * Constructs a new {@code PlatformListener} and logs a confirmation message.
     */
    public PlatformListener() {
        MessageUtils.logInfo("BaseListener registered!");
    }

    /**
     * Handles pre-login validation: enforces the whitelist, loads or creates the
     * {@link CosmicPlayer}, and fires a {@link LoginReceivedEvent}.
     *
     * <p>If the whitelist is enabled and the player is not listed, or if a module cancels
     * the {@link LoginReceivedEvent}, the connection is denied with the configured message.
     *
     * @param event the Velocity {@link PreLoginEvent}
     */
    @Subscribe
    public void onPreJoin(PreLoginEvent event) {
        InboundConnection connection = event.getConnection();
        if (! (connection instanceof Player)) return;
        Player p = (Player) connection;

        String uuid = p.getUniqueId().toString();

        WhitelistConfig whitelistConfig = GivenConfigs.getWhitelistConfig();
        if (whitelistConfig.isEnabled()) {
            WhitelistEntry entry = whitelistConfig.getEntry(uuid);
            if (entry == null) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Messenger.getInstance().codedText(MainMessagesHandler.MESSAGES.INVALID.WHITELIST_NOT.get())));
                return;
            }
        }

        CosmicPlayer streamPlayer = UserUtils.getOrGetPlayer(uuid).orElse(null);
        if (streamPlayer == null) return;
        streamPlayer.waitUntilFullyLoaded();

        streamPlayer.setCurrentName(p.getUsername());
        p.getCurrentServer().ifPresent(serverConnection -> streamPlayer.setServerName(serverConnection.getServerInfo().getName()));

        LoginReceivedEvent loginReceivedEvent = new LoginReceivedEvent(streamPlayer);
        ModuleUtils.fireEvent(loginReceivedEvent);

        if (loginReceivedEvent.getResult().isCancelled()) {
            if (! loginReceivedEvent.getResult().validate()) return;

            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Messenger.getInstance().codedText(loginReceivedEvent.getResult().getDisconnectMessage())));
        }
    }

    /**
     * Handles post-login setup: caches the player UUID, populates the {@link CosmicPlayer}
     * with IP, name, and initial server, and fires a {@link LoginCompletedEvent}.
     *
     * @param event the Velocity {@link PostLoginEvent}
     */
    @Subscribe
    public void onJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        UuidManager.cachePlayer(player.getUniqueId().toString(), player.getUsername(), UserManager.getInstance().parsePlayerIP(player.getUniqueId().toString()));

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
        if (streamPlayer == null) {
            MessageUtils.logWarning("Failed to create CosmicPlayer for " + player.getUniqueId().toString() + " (" + player.getUsername() + ")");
            return;
        }

        streamPlayer.setCurrentIp(UserManager.getInstance().parsePlayerIP(player.getUniqueId().toString()));
        streamPlayer.setCurrentName(player.getUsername());
        player.getCurrentServer().ifPresent(serverConnection -> {
            String serverName = serverConnection.getServerInfo().getName();

            streamPlayer.setServerName(serverName);

            if (GivenConfigs.getServerConfig().isAutoCorrect()) {
                AsyncUtils.runAsync(() -> ServerNameMessageBuilder.build(streamPlayer, serverName).send(), 20); // After 20 ticks (1 second)
            }
        });

        LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(streamPlayer);
        ModuleUtils.fireEvent(loginCompletedEvent);
    }

    /**
     * Handles player disconnect: fires a {@link LogoutEvent}, saves the player's data,
     * and unloads the {@link CosmicPlayer} from memory.
     *
     * @param event the Velocity {@link DisconnectEvent}
     */
    @Subscribe
    public void onLeave(DisconnectEvent event) {
        Player player = event.getPlayer();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
        if (streamPlayer == null) {
            MessageUtils.logWarning("Failed to create CosmicPlayer for " + player.getUniqueId().toString() + " (" + player.getUsername() + ")");
            return;
        }

        LogoutEvent logoutEvent = new LogoutEvent(streamPlayer);
        ModuleUtils.fireEvent(logoutEvent);

        streamPlayer.save();
        UserUtils.unloadSender(streamPlayer);
    }

    /**
     * Handles server-switch events: updates the {@link CosmicPlayer}'s recorded server name
     * and optionally triggers a server-name auto-correction message.
     *
     * @param event the Velocity {@link ServerConnectedEvent}
     */
    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
        if (streamPlayer == null) {
            MessageUtils.logWarning("Failed to create CosmicPlayer for " + player.getUniqueId().toString() + " (" + player.getUsername() + ")");
            return;
        }

        String serverName = event.getServer().getServerInfo().getName();
        streamPlayer.setServerName(serverName);

        if (GivenConfigs.getServerConfig().isAutoCorrect()) {
            AsyncUtils.runAsync(() -> ServerNameMessageBuilder.build(streamPlayer, serverName).send(), 20); // After 20 ticks (1 second)
        }
    }

    /**
     * Handles player chat messages: wraps the message in a {@link CosmicChatEvent} and fires it.
     *
     * <p>If the event is cancelled by a module the original chat event is denied;
     * otherwise the (potentially modified) message is passed through.
     *
     * @param event the Velocity {@link PlayerChatEvent}
     */
    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
        if (streamPlayer == null) {
            MessageUtils.logWarning("Failed to create CosmicPlayer for " + player.getUniqueId().toString() + " (" + player.getUsername() + ")");
            return;
        }

        CosmicChatEvent chatEvent = new CosmicChatEvent(streamPlayer, event.getMessage());
        ModuleManager.fireEvent(chatEvent);
        if (chatEvent.isCanceled()) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }
        event.setResult(PlayerChatEvent.ChatResult.message(chatEvent.getMessage()));
    }

    /**
     * Relays a cross-platform {@link CosmicEvent} that was submitted through the Velocity
     * event bus via a {@link ProperEvent} wrapper.
     *
     * @param event the {@link ProperEvent} carrying the {@link CosmicEvent} to fire
     */
    @Subscribe
    public void onProperEvent(ProperEvent event) {
        ModuleManager.fireEvent(event.getCosmicEvent());
    }

    /**
     * Handles incoming plugin messages on the StreamlineCore channel, building a
     * {@link ProxiedMessage} and firing a {@link ProxyMessageInEvent} for registered handlers.
     *
     * @param event the Velocity {@link PluginMessageEvent}
     */
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        String tag = event.getIdentifier().getId();
        if (! (event.getTarget() instanceof Player)) return;
        Player player = (Player) event.getTarget();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
        if (streamPlayer == null) {
            MessageUtils.logWarning("Failed to create CosmicPlayer for " + player.getUniqueId().toString() + " (" + player.getUsername() + ")");
            return;
        }

        try {
            ProxiedMessage messageIn = new ProxiedMessage(streamPlayer, false, event.getData(), tag);
            ProxyMessageInEvent e = new ProxyMessageInEvent(messageIn).fire();
            if (e.isCancelled()) return;
            SLAPI.getInstance().getProxyMessenger().receiveMessage(e);
        } catch (Exception e) {
            // do nothing.
        }
    }

    /**
     * Fires a cross-platform {@link ServerStartEvent} when the proxy finishes initialising.
     *
     * @param event the Velocity {@link ProxyInitializeEvent}
     */
    @Subscribe
    public void onStart(ProxyInitializeEvent event) {
        ServerStartEvent e = new ServerStartEvent().fire();
        if (e.isCancelled()) return;
        if (! e.isSendable()) return;
        SLAPI.sendConsoleMessage(e.getMessage());
    }

    /**
     * Fires a cross-platform {@link singularity.events.server.ServerStopEvent} when the proxy begins shutdown.
     *
     * @param event the Velocity {@link ProxyShutdownEvent}
     */
    @Subscribe
    public void onStop(ProxyShutdownEvent event) {
        ServerStopEvent e = new ServerStopEvent().fire();
        if (e.isCancelled()) return;
        if (! e.isSendable()) return;
        SLAPI.sendConsoleMessage(e.getMessage());
    }

    /**
     * Intercepts proxy ping requests and translates any modifications made by modules
     * through a {@link PingReceivedEvent} back into the Velocity {@link ServerPing} response.
     *
     * <p>Handles protocol version, player sample list, MOTD, and favicon modifications.
     *
     * @param event the Velocity {@link ProxyPingEvent}
     */
    @Subscribe
    public void onPing(ProxyPingEvent event) {
        ServerPing ping = event.getPing();
        String hostName;
        try {
            hostName = event.getConnection().getVirtualHost().get().getHostString();
        } catch (Throwable e) {
            hostName = "";
        }

        PingedResponse.Protocol protocol = new PingedResponse.Protocol(
                ModuleUtils.replacePlaceholders(ping.getVersion().getName()), ping.getVersion().getProtocol());
        PingedResponse.Players players;
        List<PingedResponse.PlayerInfo> infos = new ArrayList<>();
        if (ping.getPlayers().isPresent()) {
            for (ServerPing.SamplePlayer info : ping.getPlayers().get().getSample()) {
                infos.add(new PingedResponse.PlayerInfo(info.getName(), info.getId()));
            }
            players = new PingedResponse.Players(ping.getPlayers().get().getMax(), ping.getPlayers().get().getOnline(),
                    infos.toArray(new PingedResponse.PlayerInfo[0]));
        } else {
            players = new PingedResponse.Players(0, 0, new PingedResponse.PlayerInfo[0]);
        }
        PingedResponse response;
        try {
            if (ping.getFavicon().isEmpty()) {
                response = new PingedResponse(protocol, players, Messenger.getInstance().asString(ping.getDescriptionComponent()));
            } else {
                response = new PingedResponse(protocol, players, Messenger.getInstance().asString(ping.getDescriptionComponent()), ping.getFavicon().get().getBase64Url());
            }
        } catch (Throwable e) {
            MessageUtils.logWarning("Failed to get favicon from ping: " + e.getMessage());
            MessageUtils.logWarning(e.getStackTrace());
            return;
        }

        PingReceivedEvent pingReceivedEvent = new PingReceivedEvent(response, hostName).fire();

        if (pingReceivedEvent.isCancelled()) {
            return;
        }

        ServerPing.Builder builder = ping.asBuilder();

        if (pingReceivedEvent.getResponse().getVersion().getProtocol() != -1) {
            ServerPing.Version protocolServer = new ServerPing.Version(pingReceivedEvent.getResponse().getVersion().getProtocol(),
                    pingReceivedEvent.getResponse().getVersion().getName());
            builder.version(protocolServer);
        }

        ServerPing.SamplePlayer[] infosServer = new ServerPing.SamplePlayer[pingReceivedEvent.getResponse().getPlayers().getSample().length];
        for (int i = 0; i < pingReceivedEvent.getResponse().getPlayers().getSample().length; i++) {
            PingedResponse.PlayerInfo info = pingReceivedEvent.getResponse().getPlayers().getSample()[i];
            infosServer[i] = new ServerPing.SamplePlayer(MessageUtils.replaceAmpersand(ModuleUtils.replacePlaceholders(info.getName())), info.getUniqueId());
        }

        builder.samplePlayers(infosServer);
        builder.onlinePlayers(pingReceivedEvent.getResponse().getPlayers().getOnline());
        builder.maximumPlayers(pingReceivedEvent.getResponse().getPlayers().getMax());

        builder.description(Messenger.getInstance().codedText(ModuleUtils.replacePlaceholders(pingReceivedEvent.getResponse().getDescription())));

        try {
            CosmicFavicon favicon = pingReceivedEvent.getResponse().getFavicon();
            if (favicon != null) {
                Favicon fav = Favicon.create(favicon.getImage());
                builder.favicon(fav);
            }
        } catch (Exception e) {
            // do nothing.
        }

        event.setPing(builder.build());
    }

    /**
     * Handles server-kick events: fires a cross-platform {@link KickedFromServerEvent} and,
     * if a redirect server is specified, redirects the player there instead of disconnecting.
     *
     * @param event the Velocity {@link com.velocitypowered.api.event.player.KickedFromServerEvent}
     */
    @Subscribe
    public void onServerKick(com.velocitypowered.api.event.player.KickedFromServerEvent event) {
        Player player = event.getPlayer();
        RegisteredServer from = event.getServer();
        String kickedReason = event.getServerKickReason().isPresent() ? Messenger.getInstance().asString(event.getServerKickReason().get()) : "none";

        String fromName = from == null ? "none" : from.getServerInfo().getName();
        String toName;

        if (player.getCurrentServer().isPresent()) {
            toName = player.getCurrentServer().get().getServerInfo().getName();
        } else {
            toName = "none";
        }

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
        if (streamPlayer == null) {
            MessageUtils.logWarning("Failed to create CosmicPlayer for " + player.getUniqueId().toString() + " (" + player.getUsername() + ")");
            return;
        }

        KickedFromServerEvent kickedFromServerEvent = new KickedFromServerEvent(streamPlayer, fromName, kickedReason, toName).fire();

        if (kickedFromServerEvent.isCancelled()) {
            if (from != null) event.setResult(com.velocitypowered.api.event.player.KickedFromServerEvent.RedirectPlayer.create(from));
            return;
        }

        if (kickedFromServerEvent.getToServer() != null) {
            if (! kickedFromServerEvent.getToServer().equalsIgnoreCase("none")) {
                Optional<RegisteredServer> serverInfo = StreamlineVelocity.getInstance().getProxy().getServer(kickedFromServerEvent.getToServer());
                serverInfo.ifPresent(registeredServer ->
                        event.setResult(com.velocitypowered.api.event.player.KickedFromServerEvent.RedirectPlayer.create(registeredServer)));
            }
        }
    }
}
