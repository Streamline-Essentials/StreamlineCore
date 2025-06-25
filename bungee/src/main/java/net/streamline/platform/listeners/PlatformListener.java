package net.streamline.platform.listeners;

import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.streamline.api.SLAPI;
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
import singularity.objects.CosmicFavicon;
import singularity.objects.PingedResponse;
import net.streamline.base.StreamlineBungee;
import net.streamline.platform.Messenger;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.savables.UserManager;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlatformListener implements Listener {
    public PlatformListener() {
        MessageUtils.logInfo("BaseListener registered!");
    }

    @EventHandler
    public void onPreJoin(PreLoginEvent event) {
        PendingConnection connection = event.getConnection();
        if (connection == null) return;

        String name = connection.getName();
        Optional<String> optional = UuidManager.getUuidFromName(name);
        String uuid;
        if (optional.isEmpty()) {
            ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(name);
            if (proxiedPlayer == null) return;

            uuid = proxiedPlayer.getUniqueId().toString();
        } else {
            uuid = optional.get();
        }

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(uuid);

        streamPlayer.setCurrentName(name);
//        streamPlayer.getLocation().setServerName(proxiedPlayer.getServer().getInfo().getName());

        WhitelistConfig whitelistConfig = GivenConfigs.getWhitelistConfig();
        if (whitelistConfig.isEnabled()) {
            WhitelistEntry entry = whitelistConfig.getEntry(streamPlayer.getUuid());
            if (entry == null) {
                event.setCancelReason(Messenger.getInstance().codedText(MainMessagesHandler.MESSAGES.INVALID.WHITELIST_NOT.get()));
                event.setCancelled(true);
                return;
            }
        }

        LoginReceivedEvent loginReceivedEvent = new LoginReceivedEvent(streamPlayer);
        ModuleUtils.fireEvent(loginReceivedEvent);

        if (loginReceivedEvent.getResult().isCancelled()) {
            if (! loginReceivedEvent.getResult().validate()) return;

            event.setCancelReason(Messenger.getInstance().codedText(loginReceivedEvent.getResult().getDisconnectMessage()));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        UuidManager.cachePlayer(player.getUniqueId().toString(), player.getName(), UserManager.getInstance().parsePlayerIP(player));

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        streamPlayer.setCurrentIp(UserManager.getInstance().parsePlayerIP(player));
        streamPlayer.setCurrentName(player.getName());
        Server server = player.getServer();
        if (server != null) streamPlayer.setServerName(server.getInfo().getName());

        LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(streamPlayer);
        ModuleUtils.fireEvent(loginCompletedEvent);
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        LogoutEvent logoutEvent = new LogoutEvent(streamPlayer);
        ModuleUtils.fireEvent(logoutEvent);

        streamPlayer.save();
        UserUtils.unloadSender(streamPlayer);
    }

    @EventHandler
    public void onServerSwitch(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        streamPlayer.setServerName(event.getServer().getInfo().getName());
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        CosmicChatEvent chatEvent = new CosmicChatEvent(streamPlayer, event.getMessage());
        StreamlineBungee.getInstance().fireEvent(chatEvent, true);
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

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (! (event.getReceiver() instanceof ProxiedPlayer)) return;
        ProxiedPlayer player = ((ProxiedPlayer) event.getReceiver());
        String tag = event.getTag();
        if (event.getData() == null) return;

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

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        ServerPing ping = event.getResponse();
        String hostName;
        try {
            hostName = event.getConnection().getVirtualHost().getHostString();
        } catch (Throwable e) {
            hostName = "";
        }

        PingedResponse.Protocol protocol = new PingedResponse.Protocol(ping.getVersion().getName(), ping.getVersion().getProtocol());
        List<PingedResponse.PlayerInfo> infos = new ArrayList<>();
        if (ping.getPlayers() != null) {
            if (ping.getPlayers().getSample() != null) {
                for (ServerPing.PlayerInfo info : ping.getPlayers().getSample()) {
                    infos.add(new PingedResponse.PlayerInfo(info.getName(), info.getId()));
                }
            }
        }
        PingedResponse.Players players = new PingedResponse.Players(ping.getPlayers().getMax(), ping.getPlayers().getOnline(),
                infos.toArray(new PingedResponse.PlayerInfo[0]));
        PingedResponse response;
        try {
            if (ping.getFaviconObject() != null) {
                response = new PingedResponse(protocol, players, ping.getDescriptionComponent().toLegacyText(), ping.getFaviconObject().getEncoded());
            } else {
                response = new PingedResponse(protocol, players, ping.getDescriptionComponent().toLegacyText());
            }
        } catch (Throwable e) {
            MessageUtils.logWarning("Failed to get favicon from ping response: " + e.getMessage());
            MessageUtils.logWarning(e.getStackTrace());

            return;
        }

        PingReceivedEvent pingReceivedEvent = new PingReceivedEvent(response, hostName).fire();

        if (pingReceivedEvent.isCancelled()) {
            return;
        }

        if (pingReceivedEvent.getResponse().getVersion().getProtocol() != -1) {
            ServerPing.Protocol protocolServer = new ServerPing.Protocol(pingReceivedEvent.getResponse().getVersion().getName(),
                    pingReceivedEvent.getResponse().getVersion().getProtocol());
            ping.setVersion(protocolServer);
        }

        ServerPing.PlayerInfo[] infosServer = new ServerPing.PlayerInfo[pingReceivedEvent.getResponse().getPlayers().getSample().length];
        for (int i = 0; i < pingReceivedEvent.getResponse().getPlayers().getSample().length; i++) {
            PingedResponse.PlayerInfo info = pingReceivedEvent.getResponse().getPlayers().getSample()[i];
            infosServer[i] = new ServerPing.PlayerInfo(info.getName(), info.getId());
        }
        ServerPing.Players playersServer = new ServerPing.Players(pingReceivedEvent.getResponse().getPlayers().getMax(),
                pingReceivedEvent.getResponse().getPlayers().getOnline(), infosServer);

        ping.setPlayers(playersServer);

        ping.setDescriptionComponent(new TextComponent(Messenger.getInstance().codedText(pingReceivedEvent.getResponse().getDescription())));

        try {
            CosmicFavicon favicon = pingReceivedEvent.getResponse().getFavicon();
            if (favicon != null) {
                Favicon fav = Favicon.create(favicon.getImage());
                ping.setFavicon(fav);
            }
        } catch (Exception e) {
            // do nothing.
        }

        event.setResponse(ping);
    }

    @EventHandler
    public void onServerKick(ServerKickEvent event) {
        ProxiedPlayer player = event.getPlayer();
        ServerInfo from = event.getKickedFrom();
        String kickedReason = event.getKickReason();

        String fromName = from == null ? "none" : from.getName();
        String toName;

        if (player.getServer() != null) {
            toName = player.getServer().getInfo().getName();
        } else {
            toName = "none";
        }

        CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

        KickedFromServerEvent kickedFromServerEvent = new KickedFromServerEvent(streamPlayer, fromName, kickedReason, toName);
        ModuleUtils.fireEvent(kickedFromServerEvent);

        if (kickedFromServerEvent.isCancelled()) {
            MessageUtils.logDebug("Server " + fromName + " kicked " + player.getName() + " for " + kickedReason + " but the event was cancelled.");
            return;
        }

        event.setKickReason(kickedFromServerEvent.getReason());

        if (kickedFromServerEvent.getToServer() != null) {
            if (! kickedFromServerEvent.getToServer().equalsIgnoreCase("none")) {
                ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(kickedFromServerEvent.getToServer());
                if (serverInfo != null) {
                    event.setCancelled(true);
                    event.setCancelServer(serverInfo);
                    MessageUtils.logDebug("Server " + fromName + " kicked " + player.getName() + " for " + kickedReason + " and sent them to " + kickedFromServerEvent.getToServer());
                } else {
                    MessageUtils.logDebug("Server " + fromName + " kicked " + player.getName() + " for " + kickedReason + " but the server " + kickedFromServerEvent.getToServer() + " was not found.");
                }
            } else {
                MessageUtils.logDebug("Server " + fromName + " kicked " + player.getName() + " for " + kickedReason + " but no server was specified to send them to.");
            }
        }
    }
}
