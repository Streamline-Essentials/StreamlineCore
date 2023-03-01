package net.streamline.platform.listeners;

import tv.quaint.thebase.lib.google.common.io.ByteArrayDataInput;
import tv.quaint.thebase.lib.google.common.io.ByteStreams;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.CachedUUIDsHandler;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.configs.given.whitelist.WhitelistConfig;
import net.streamline.api.configs.given.whitelist.WhitelistEntry;
import net.streamline.api.events.server.*;
import net.streamline.api.events.server.ping.PingReceivedEvent;
import net.streamline.api.messages.builders.SavablePlayerMessageBuilder;
import net.streamline.api.messages.builders.UserNameMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
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

import java.util.ArrayList;
import java.util.List;

public class PlatformListener implements Listener {
    public PlatformListener() {
        MessageUtils.logInfo("BaseListener registered!");
    }

    @EventHandler
    public void onPreJoin(PreLoginEvent event) {
        PendingConnection connection = event.getConnection();

        if (connection == null) return;

        StreamlineUser user = UserUtils.getOrGetUserByName(connection.getName());
        if (! (user instanceof StreamlinePlayer)) return;
        StreamlinePlayer player = ((StreamlinePlayer) user);

        if (connection instanceof ProxiedPlayer) {
            try {
                ProxiedPlayer proxiedPlayer = (ProxiedPlayer) connection;
                player.setLatestName(proxiedPlayer.getName());
                player.setLatestServer(proxiedPlayer.getServer().getInfo().getName());
            } catch (Exception e) {
//                e.printStackTrace(); // no errors in console.
            }
        }

        WhitelistConfig whitelistConfig = GivenConfigs.getWhitelistConfig();
        if (whitelistConfig.isEnabled()) {
            WhitelistEntry entry = whitelistConfig.getEntry(player.getUuid());
            if (entry == null) {
                event.setCancelReason(Messenger.getInstance().codedText(MainMessagesHandler.MESSAGES.INVALID.WHITELIST_NOT.get()));
                event.setCancelled(true);
                return;
            }
        }

        LoginReceivedEvent loginReceivedEvent = new LoginReceivedEvent(player);
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

        CachedUUIDsHandler.cachePlayer(player.getUniqueId().toString(), player.getName());

        StreamlinePlayer streamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
        streamlinePlayer.setLatestIP(UserManager.getInstance().parsePlayerIP(player));
        streamlinePlayer.setLatestName(event.getPlayer().getName());
        streamlinePlayer.setLatestServer(event.getPlayer().getServer().getInfo().getName());

        LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(streamlinePlayer);
        ModuleUtils.fireEvent(loginCompletedEvent);

        UserNameMessageBuilder.build(streamlinePlayer, streamlinePlayer.getDisplayName(), streamlinePlayer).send();
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        StreamlinePlayer StreamlinePlayer = UserUtils.getOrGetPlayer(uuid);
        if (StreamlinePlayer == null) return;

        LogoutEvent logoutEvent = new LogoutEvent(StreamlinePlayer);
        ModuleUtils.fireEvent(logoutEvent);

        StreamlinePlayer.getStorageResource().sync();
        UserUtils.unloadUser(StreamlinePlayer);
    }

    @EventHandler
    public void onServerSwitch(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();

        StreamlinePlayer streamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
        streamlinePlayer.setLatestServer(event.getServer().getInfo().getName());

        new BaseRunnable(20, 1) {
            @Override
            public void run() {
                SavablePlayerMessageBuilder.build(streamlinePlayer, true).send();
                UserNameMessageBuilder.build(streamlinePlayer, streamlinePlayer.getDisplayName(), streamlinePlayer).send();
                this.cancel();
            }
        };
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();

        StreamlinePlayer StreamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
        StreamlineChatEvent chatEvent = new StreamlineChatEvent(StreamlinePlayer, event.getMessage());
        ModuleManager.fireEvent(chatEvent);
        if (chatEvent.isCanceled()) {
            event.setCancelled(true);
            return;
        }
        event.setMessage(chatEvent.getMessage());
    }

    @EventHandler
    public void onProperEvent(ProperEvent event) {
        ModuleManager.fireEvent(event.getStreamlineEvent());
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (! (event.getReceiver() instanceof ProxiedPlayer)) return;
        ProxiedPlayer player = ((ProxiedPlayer) event.getReceiver());

        StreamlinePlayer streamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);

        String tag = event.getTag();
        if (event.getData() == null) return;

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
        PingedResponse response = new PingedResponse(protocol, players, ping.getDescriptionComponent().toLegacyText(), ping.getFaviconObject().getEncoded());

        PingReceivedEvent pingReceivedEvent = new PingReceivedEvent(response).fire();

        if (pingReceivedEvent.isCancelled()) {
            return;
        }

        ServerPing.Protocol protocolServer = new ServerPing.Protocol(pingReceivedEvent.getResponse().getVersion().getName(),
                pingReceivedEvent.getResponse().getVersion().getProtocol());
        ping.setVersion(protocolServer);

        ServerPing.PlayerInfo[] infosServer = new ServerPing.PlayerInfo[pingReceivedEvent.getResponse().getPlayers().getSample().length];
        for (int i = 0; i < pingReceivedEvent.getResponse().getPlayers().getSample().length; i++) {
            PingedResponse.PlayerInfo info = pingReceivedEvent.getResponse().getPlayers().getSample()[i];
            infosServer[i] = new ServerPing.PlayerInfo(info.getName(), info.getId());
        }
        ServerPing.Players playersServer = new ServerPing.Players(pingReceivedEvent.getResponse().getPlayers().getMax(),
                pingReceivedEvent.getResponse().getPlayers().getOnline(), infosServer);

        ping.setPlayers(playersServer);

        ping.setDescriptionComponent(Messenger.getInstance().codedText(pingReceivedEvent.getResponse().getDescription()));

        try {
            Favicon favicon = Favicon.create(pingReceivedEvent.getResponse().getFavicon().getEncoded());
            ping.setFavicon(favicon);
        } catch (Exception e) {
            // do nothing.
        }

        event.setResponse(ping);
    }
}
