package net.streamline.platform.listeners;

import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.configs.given.whitelist.WhitelistConfig;
import net.streamline.api.configs.given.whitelist.WhitelistEntry;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.uuid.UuidManager;
import net.streamline.api.events.server.*;
import net.streamline.api.events.server.ping.PingReceivedEvent;
import net.streamline.api.messages.builders.StreamPlayerMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.PingedResponse;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.Messenger;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.savables.UserManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

        StreamPlayer streamPlayer = UserUtils.getOrCreatePlayerAsync(uuid).join();

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

        streamPlayer.save();
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        UuidManager.cachePlayer(player.getUniqueId().toString(), player.getName(), UserManager.getInstance().parsePlayerIP(player));

        CompletableFuture.runAsync(() -> {
            StreamPlayer streamPlayer = UserUtils.getOrCreatePlayerAsync(player.getUniqueId().toString()).join();

            streamPlayer.setCurrentIp(UserManager.getInstance().parsePlayerIP(player));
            streamPlayer.setCurrentName(player.getName());
            Server server = player.getServer();
            if (server != null) streamPlayer.setServerName(server.getInfo().getName());

            LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(streamPlayer);
            ModuleUtils.fireEvent(loginCompletedEvent);

            streamPlayer.save();
        });
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        CompletableFuture.runAsync(() -> {
            StreamPlayer streamPlayer = UserUtils.getOrCreatePlayerAsync(player.getUniqueId().toString()).join();

            LogoutEvent logoutEvent = new LogoutEvent(streamPlayer);
            ModuleUtils.fireEvent(logoutEvent);

            streamPlayer.save();
            UserUtils.unloadSender(streamPlayer);
        });
    }

    @EventHandler
    public void onServerSwitch(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();

        CompletableFuture.runAsync(() -> {
            StreamPlayer streamPlayer = UserUtils.getOrCreatePlayerAsync(player.getUniqueId().toString()).join();

            streamPlayer.setServerName(event.getServer().getInfo().getName());

            new BaseRunnable(20, 1) {
                @Override
                public void run() {
                    StreamPlayerMessageBuilder.build(streamPlayer, true).send();
                    this.cancel();
                }
            };

            streamPlayer.save();
        });
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();

        CompletableFuture.runAsync(() -> {
            StreamPlayer streamPlayer = UserUtils.getOrCreatePlayerAsync(player.getUniqueId().toString()).join();

            StreamlineChatEvent chatEvent = new StreamlineChatEvent(streamPlayer, event.getMessage());
            Streamline.getInstance().fireEvent(chatEvent, true);
            if (chatEvent.isCanceled()) {
                event.setCancelled(true);
                return;
            }
            event.setMessage(chatEvent.getMessage());

            streamPlayer.save();
        }).join();
    }

    @EventHandler
    public void onProperEvent(ProperEvent event) {
        ModuleManager.fireEvent(event.getStreamlineEvent());
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (! (event.getReceiver() instanceof ProxiedPlayer)) return;
        ProxiedPlayer player = ((ProxiedPlayer) event.getReceiver());
        String tag = event.getTag();
        if (event.getData() == null) return;

        StreamPlayer streamPlayer = UserUtils.getOrCreatePlayerAsync(player.getUniqueId().toString()).join();

        try {
            ProxiedMessage messageIn = new ProxiedMessage(streamPlayer, true, event.getData(), tag);
            ProxyMessageInEvent e = new ProxyMessageInEvent(messageIn);
            ModuleUtils.fireEvent(e);
            if (e.isCancelled()) return;
            SLAPI.getInstance().getProxyMessenger().receiveMessage(e);
        } catch (Exception e) {
            // do nothing.
        }
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        ServerPing ping = event.getResponse();

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
        if (ping.getFaviconObject() != null) {
            response = new PingedResponse(protocol, players, ping.getDescriptionComponent().toLegacyText(), ping.getFaviconObject().getEncoded());
        } else {
            response = new PingedResponse(protocol, players, ping.getDescriptionComponent().toLegacyText());
        }

        PingReceivedEvent pingReceivedEvent = new PingReceivedEvent(response).fire();

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
            Favicon favicon = Favicon.create(pingReceivedEvent.getResponse().getFavicon().getEncoded());
            ping.setFavicon(favicon);
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

        CompletableFuture.runAsync(() -> {
            StreamPlayer streamPlayer = UserUtils.getOrCreatePlayerAsync(player.getUniqueId().toString()).join();

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

            streamPlayer.save();
        });
    }
}
