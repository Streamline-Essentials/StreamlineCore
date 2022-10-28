package net.streamline.platform.listeners;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.CachedUUIDsHandler;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.configs.given.whitelist.WhitelistConfig;
import net.streamline.api.configs.given.whitelist.WhitelistEntry;
import net.streamline.api.events.server.*;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.events.ProperEvent;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.platform.savables.UserManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class PlatformListener implements Listener {
    @Getter @Setter
    private static boolean messaged = false;
    @Getter @Setter
    private static boolean joined = false;

    public static boolean isTested() {
        return isMessaged() && isJoined();
    }

    public PlatformListener() {
        MessageUtils.logInfo("BaseListener registered!");
        ModuleUtils.listen(this, SLAPI.getBaseModule());
    }

    @EventHandler
    public void onPreJoin(AsyncPlayerPreLoginEvent event) {
        StreamlineUser user = UserUtils.getOrGetUserByName(event.getName());
        if (! (user instanceof StreamlinePlayer player)) return;

        WhitelistConfig whitelistConfig = GivenConfigs.getWhitelistConfig();
        if (whitelistConfig.isEnabled()) {
            WhitelistEntry entry = whitelistConfig.getEntry(player.getUuid());
            if (entry == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, MessageUtils.codedString(MainMessagesHandler.MESSAGES.INVALID.WHITELIST_NOT.get()));
                return;
            }
        }

        LoginReceivedEvent loginReceivedEvent = new LoginReceivedEvent(player);
        StreamEventHandler.fireEvent(loginReceivedEvent);

        if (loginReceivedEvent.getResult().isCancelled()) {
            if (! loginReceivedEvent.getResult().validate()) return;

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, MessageUtils.codedString(loginReceivedEvent.getResult().getDisconnectMessage()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        CachedUUIDsHandler.cachePlayer(player.getUniqueId().toString(), player.getName());

        if (! SLAPI.isProxiedServer() && isTested()) {
            StreamlinePlayer streamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
            streamlinePlayer.setLatestIP(UserManager.getInstance().parsePlayerIP(player));
            streamlinePlayer.setLatestName(player.getName());

            LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(streamlinePlayer);
            ModuleUtils.fireEvent(loginCompletedEvent);
        }

        setJoined(true);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        StreamlinePlayer StreamlinePlayer = UserUtils.getOrGetPlayer(uuid);
        if (StreamlinePlayer == null) return;

        LogoutEvent logoutEvent = new LogoutEvent(StreamlinePlayer);
        ModuleUtils.fireEvent(logoutEvent);

        StreamlinePlayer.getStorageResource().sync();
        UserUtils.unloadUser(StreamlinePlayer);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        StreamlinePlayer StreamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
        StreamlineChatEvent chatEvent = new StreamlineChatEvent(StreamlinePlayer, event.getMessage());
        Streamline.getInstance().fireEvent(chatEvent, true);
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

    public static class ProxyMessagingListener implements PluginMessageListener {
        public ProxyMessagingListener() {
            MessageUtils.logInfo("Registered " + getClass().getSimpleName() + "!");
        }

        @Override
        public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
            StreamlinePlayer streamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);

            try {
                ProxiedMessage messageIn = new ProxiedMessage(streamlinePlayer, true, message, channel);
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
        ModuleUtils.sendMessage(ModuleUtils.getConsole(), e.getMessage());
    }

    public void onProxiedMessageReceived(ProxyMessageInEvent event) {
        setMessaged(true);
    }
}
