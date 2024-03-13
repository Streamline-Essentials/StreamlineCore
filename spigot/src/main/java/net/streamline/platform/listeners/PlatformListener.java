package net.streamline.platform.listeners;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.configs.given.whitelist.WhitelistConfig;
import net.streamline.api.configs.given.whitelist.WhitelistEntry;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.uuid.UuidManager;
import net.streamline.api.events.server.*;
import net.streamline.api.events.server.ping.PingReceivedEvent;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.PingedResponse;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.base.TenSecondTimer;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.savables.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.NotNull;
import tv.quaint.events.BaseEventHandler;
import tv.quaint.events.BaseEventListener;
import tv.quaint.events.processing.BaseProcessor;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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

    public PlatformListener() {
        MessageUtils.logInfo("BaseListener registered!");
        setProcessorListener(new BaseProcessorListener());
        ModuleUtils.listen(getProcessorListener(), SLAPI.getBaseModule());
    }

    @EventHandler
    public void onPreJoin(AsyncPlayerPreLoginEvent event) {
        String uuid = event.getUniqueId().toString();

        StreamPlayer streamPlayer = UserUtils.getOrCreatePlayerAsync(uuid).join();

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

        streamPlayer.save();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        UuidManager.cachePlayer(player.getUniqueId().toString(), player.getName(), UserManager.getInstance().parsePlayerIP(player));


        CompletableFuture.runAsync(() -> {
            StreamPlayer streamPlayer = UserUtils.getOrCreatePlayerAsync(player.getUniqueId().toString()).join();

            streamPlayer.setCurrentIp(UserManager.getInstance().parsePlayerIP(player));
            streamPlayer.setCurrentName(player.getName());

            LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(streamPlayer);
            ModuleUtils.fireEvent(loginCompletedEvent);

            setJoined(true);

            new TenSecondTimer(player);

            streamPlayer.save();
        });
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        UuidManager.cachePlayer(player.getUniqueId().toString(), player.getName(), UserManager.getInstance().parsePlayerIP(player.getUniqueId().toString()));

        CompletableFuture.runAsync(() -> {
            StreamPlayer streamPlayer = UserUtils.getOrCreatePlayerAsync(player.getUniqueId().toString()).join();

            LogoutEvent logoutEvent = new LogoutEvent(streamPlayer);
            ModuleUtils.fireEvent(logoutEvent);

            streamPlayer.save();
            UserUtils.unloadSender(streamPlayer);
        });
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

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

    public static class ProxyMessagingListener implements PluginMessageListener {
        public ProxyMessagingListener() {
            MessageUtils.logInfo("Registered " + getClass().getSimpleName() + "!");
        }

        @Override
        public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
            StreamPlayer streamPlayer = UserUtils.getOrCreatePlayerAsync(player.getUniqueId().toString()).join();

            try {
                ProxiedMessage messageIn = new ProxiedMessage(streamPlayer, true, message, channel);
                ProxyMessageInEvent e = new ProxyMessageInEvent(messageIn);
                ModuleUtils.fireEvent(e);
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
        PingedResponse.Protocol protocol = new PingedResponse.Protocol("latest", 1);

        List<PingedResponse.PlayerInfo> playerInfos = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerInfos.add(new PingedResponse.PlayerInfo(player.getName(), player.getUniqueId().toString()));
        }

        PingedResponse.Players players = new PingedResponse.Players(event.getMaxPlayers(), event.getNumPlayers(),
                playerInfos.toArray(new PingedResponse.PlayerInfo[0]));

        PingedResponse response = new PingedResponse(protocol, players, event.getMotd());

        PingReceivedEvent pingReceivedEvent = new PingReceivedEvent(response).fire();

        if (pingReceivedEvent.isCancelled()) {
            return;
        }

        event.setMotd(pingReceivedEvent.getResponse().getDescription());
        event.setMaxPlayers(pingReceivedEvent.getResponse().getPlayers().getMax());
//        event.setNumPlayers(pingReceivedEvent.getResponse().getPlayers().getOnline());

        try {
            CachedServerIcon icon = Bukkit.loadServerIcon(Paths.get(pingReceivedEvent.getResponse().getFaviconString()).toFile());
            event.setServerIcon(icon);
        } catch (Exception e) {
            // do nothing.
        }
    }
}
