package net.streamline.platform.listeners;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.streamline.base.Streamline;
import tv.quaint.thebase.lib.google.common.io.ByteArrayDataInput;
import tv.quaint.thebase.lib.google.common.io.ByteStreams;
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
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.CachedUUIDsHandler;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.configs.given.whitelist.WhitelistConfig;
import net.streamline.api.configs.given.whitelist.WhitelistEntry;
import net.streamline.api.events.server.*;
import net.streamline.api.events.server.ping.PingReceivedEvent;
import net.streamline.api.messages.builders.UserNameMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.builders.SavablePlayerMessageBuilder;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.PingedResponse;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.platform.Messenger;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.savables.UserManager;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlatformListener {
    public PlatformListener() {
        MessageUtils.logInfo("BaseListener registered!");
    }

    @Subscribe
    public void onPreJoin(PreLoginEvent event) {
        InboundConnection connection = event.getConnection();
        if (! (connection instanceof Player)) return;
        Player p = (Player) connection;

        StreamlineUser user = UserUtils.getOrGetUserByName(p.getUsername());
        if (! (user instanceof StreamlinePlayer)) return;
        StreamlinePlayer player = ((StreamlinePlayer) user);
        p.getCurrentServer().ifPresent(serverConnection -> player.setLatestServer(serverConnection.getServerInfo().getName()));

        WhitelistConfig whitelistConfig = GivenConfigs.getWhitelistConfig();
        if (whitelistConfig.isEnabled()) {
            WhitelistEntry entry = whitelistConfig.getEntry(player.getUuid());
            if (entry == null) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Messenger.getInstance().codedText(MainMessagesHandler.MESSAGES.INVALID.WHITELIST_NOT.get())));
                return;
            }
        }

        LoginReceivedEvent loginReceivedEvent = new LoginReceivedEvent(player);
        ModuleUtils.fireEvent(loginReceivedEvent);

        if (loginReceivedEvent.getResult().isCancelled()) {
            if (! loginReceivedEvent.getResult().validate()) return;

            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Messenger.getInstance().codedText(loginReceivedEvent.getResult().getDisconnectMessage())));
        }
    }

    @Subscribe
    public void onJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        CachedUUIDsHandler.cachePlayer(player.getUniqueId().toString(), player.getUsername());

        StreamlinePlayer streamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
        streamlinePlayer.setLatestIP(UserManager.getInstance().parsePlayerIP(player.getUniqueId().toString()));
        streamlinePlayer.setLatestName(event.getPlayer().getUsername());
        player.getCurrentServer().ifPresent(serverConnection -> streamlinePlayer.setLatestServer(serverConnection.getServerInfo().getName()));

        LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(streamlinePlayer);
        ModuleUtils.fireEvent(loginCompletedEvent);

        UserNameMessageBuilder.build(streamlinePlayer, streamlinePlayer.getDisplayName(), streamlinePlayer).send();
    }

    @Subscribe
    public void onLeave(DisconnectEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        StreamlinePlayer StreamlinePlayer = UserUtils.getOrGetPlayer(uuid);
        if (StreamlinePlayer == null) return;

        LogoutEvent logoutEvent = new LogoutEvent(StreamlinePlayer);
        ModuleUtils.fireEvent(logoutEvent);

        StreamlinePlayer.getStorageResource().sync();
        UserUtils.unloadUser(StreamlinePlayer);
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        StreamlinePlayer streamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
        streamlinePlayer.setLatestServer(event.getServer().getServerInfo().getName());

        new BaseRunnable(20, 1) {
            @Override
            public void run() {
                SavablePlayerMessageBuilder.build(streamlinePlayer, true).send();
                UserNameMessageBuilder.build(streamlinePlayer, streamlinePlayer.getDisplayName(), streamlinePlayer).send();
                this.cancel();
            }
        };
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();

        event.setResult(PlayerChatEvent.ChatResult.message("t"));

        StreamlinePlayer StreamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
        StreamlineChatEvent chatEvent = new StreamlineChatEvent(StreamlinePlayer, event.getMessage());
        ModuleManager.fireEvent(chatEvent);

        if (chatEvent.isCanceled()) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }
        // TODO: Change back once Velocity fixes it.
        event.setResult(PlayerChatEvent.ChatResult.message(chatEvent.getMessage()));
        if (event.getResult().getMessage().isPresent()) {
            String newMessage = event.getResult().getMessage().get();
            event.setResult(PlayerChatEvent.ChatResult.denied());
            player.spoofChatInput(newMessage);
        }
    }

    @Subscribe
    public void onProperEvent(ProperEvent event) {
        ModuleManager.fireEvent(event.getStreamlineEvent());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        String tag = event.getIdentifier().getId();
        if (! (event.getTarget() instanceof Player)) return;
        Player player = (Player) event.getTarget();
        StreamlinePlayer streamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);

        try {
            ProxiedMessage messageIn = new ProxiedMessage(streamlinePlayer, false, event.getData(), tag);
            ProxyMessageInEvent e = new ProxyMessageInEvent(messageIn);
            ModuleUtils.fireEvent(e);
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
        ModuleUtils.sendMessage(ModuleUtils.getConsole(), e.getMessage());
    }

    @Subscribe
    public void onStop(ProxyShutdownEvent event) {
        ServerStopEvent e = new ServerStopEvent().fire();
        if (e.isCancelled()) return;
        if (! e.isSendable()) return;
        ModuleUtils.sendMessage(ModuleUtils.getConsole(), e.getMessage());
    }

    @Subscribe
    public void onPing(ProxyPingEvent event) {
        ServerPing ping = event.getPing();

        PingedResponse.Protocol protocol = new PingedResponse.Protocol(ping.getVersion().getName(), ping.getVersion().getProtocol());
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

        PingReceivedEvent pingReceivedEvent = new PingReceivedEvent(response).fire();

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
            infosServer[i] = new ServerPing.SamplePlayer(info.getName(), info.getUniqueId());
        }

        builder.samplePlayers(infosServer);
        builder.onlinePlayers(pingReceivedEvent.getResponse().getPlayers().getOnline());
        builder.maximumPlayers(pingReceivedEvent.getResponse().getPlayers().getMax());

        builder.description(Messenger.getInstance().codedText(pingReceivedEvent.getResponse().getDescription()));

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
        String toName = "none";

        StreamlinePlayer streamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);

        KickedFromServerEvent kickedFromServerEvent = new KickedFromServerEvent(streamlinePlayer, fromName, kickedReason, toName).fire();

        if (kickedFromServerEvent.isCancelled()) {
            return;
        }

        if (kickedFromServerEvent.getToServer() != null) {
            if (! kickedFromServerEvent.getToServer().equalsIgnoreCase("none")) {
                Optional<RegisteredServer> serverInfo = Streamline.getInstance().getProxy().getServer(kickedFromServerEvent.getToServer());
                serverInfo.ifPresent(registeredServer -> event.setResult(com.velocitypowered.api.event.player.KickedFromServerEvent.RedirectPlayer.create(registeredServer)));
            }
        }
    }
}
