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
import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessage;
import singularity.modules.ModuleManager;
import singularity.modules.ModuleUtils;
import singularity.objects.PingedResponse;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlatformListener {
    public PlatformListener() {
        MessageUtils.logInfo("BaseListener registered!");
    }

    @Subscribe
    public void onPreJoin(PreLoginEvent event) {
        InboundConnection connection = event.getConnection();
        if (! (connection instanceof Player)) return;
        Player p = (Player) connection;

        String uuid = p.getUniqueId().toString();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(uuid);

        streamPlayer.setCurrentName(p.getUsername());
        p.getCurrentServer().ifPresent(serverConnection -> streamPlayer.setServerName(serverConnection.getServerInfo().getName()));

        WhitelistConfig whitelistConfig = GivenConfigs.getWhitelistConfig();
        if (whitelistConfig.isEnabled()) {
            WhitelistEntry entry = whitelistConfig.getEntry(streamPlayer.getUuid());
            if (entry == null) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Messenger.getInstance().codedText(MainMessagesHandler.MESSAGES.INVALID.WHITELIST_NOT.get())));
                return;
            }
        }

        LoginReceivedEvent loginReceivedEvent = new LoginReceivedEvent(streamPlayer);
        ModuleUtils.fireEvent(loginReceivedEvent);

        if (loginReceivedEvent.getResult().isCancelled()) {
            if (! loginReceivedEvent.getResult().validate()) return;

            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Messenger.getInstance().codedText(loginReceivedEvent.getResult().getDisconnectMessage())));
        }
    }

    @Subscribe
    public void onJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        UuidManager.cachePlayer(player.getUniqueId().toString(), player.getUsername(), UserManager.getInstance().parsePlayerIP(player.getUniqueId().toString()));

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        streamPlayer.setCurrentIp(UserManager.getInstance().parsePlayerIP(player.getUniqueId().toString()));
        streamPlayer.setCurrentName(player.getUsername());
        player.getCurrentServer().ifPresent(serverConnection -> streamPlayer.setServerName(serverConnection.getServerInfo().getName()));

        LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(streamPlayer);
        ModuleUtils.fireEvent(loginCompletedEvent);
    }

    @Subscribe
    public void onLeave(DisconnectEvent event) {
        Player player = event.getPlayer();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        LogoutEvent logoutEvent = new LogoutEvent(streamPlayer);
        ModuleUtils.fireEvent(logoutEvent);

        streamPlayer.save();
        UserUtils.unloadSender(streamPlayer);
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        streamPlayer.setServerName(event.getServer().getServerInfo().getName());
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        CosmicChatEvent chatEvent = new CosmicChatEvent(streamPlayer, event.getMessage());
        ModuleManager.fireEvent(chatEvent);
        if (chatEvent.isCanceled()) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }
        event.setResult(PlayerChatEvent.ChatResult.message(chatEvent.getMessage()));
    }

    @Subscribe
    public void onProperEvent(ProperEvent event) {
        ModuleManager.fireEvent(event.getCosmicEvent());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        String tag = event.getIdentifier().getId();
        if (! (event.getTarget() instanceof Player)) return;
        Player player = (Player) event.getTarget();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        try {
            ProxiedMessage messageIn = new ProxiedMessage(streamPlayer, false, event.getData(), tag);
            ProxyMessageInEvent e = new ProxyMessageInEvent(messageIn).fire();
            if (e.isCancelled()) return;
            SLAPI.getInstance().getProxyMessenger().receiveMessage(e);
        } catch (Exception e) {
            // do nothing.
        }
    }

    @Subscribe
    public void onStart(ProxyInitializeEvent event) {
        ServerStartEvent e = new ServerStartEvent().fire();
        if (e.isCancelled()) return;
        if (! e.isSendable()) return;
        SLAPI.sendConsoleMessage(e.getMessage());
    }

    @Subscribe
    public void onStop(ProxyShutdownEvent event) {
        ServerStopEvent e = new ServerStopEvent().fire();
        if (e.isCancelled()) return;
        if (! e.isSendable()) return;
        SLAPI.sendConsoleMessage(e.getMessage());
    }

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
        if (ping.getFavicon().isEmpty()) {
            response = new PingedResponse(protocol, players, Messenger.getInstance().asString(ping.getDescriptionComponent()));
        } else {
            response = new PingedResponse(protocol, players, Messenger.getInstance().asString(ping.getDescriptionComponent()), ping.getFavicon().toString());
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
            Favicon favicon = Favicon.create(Paths.get(pingReceivedEvent.getResponse().getFavicon().getEncoded()));
            builder.favicon(favicon);
        } catch (Exception e) {
            // do nothing.
        }

        event.setPing(builder.build());
    }

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

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

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
