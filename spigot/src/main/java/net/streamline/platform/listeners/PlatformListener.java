package net.streamline.platform.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.configs.given.whitelist.WhitelistConfig;
import net.streamline.api.configs.given.whitelist.WhitelistEntry;
import net.streamline.api.messages.ProxyMessageEvent;
import net.streamline.api.messages.ProxyMessageIn;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;
import net.streamline.platform.events.ProperEvent;
import net.streamline.api.events.server.LogoutEvent;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.platform.savables.UserManager;
import net.streamline.api.events.server.LoginReceivedEvent;
import net.streamline.api.events.server.StreamlineChatEvent;
import net.streamline.platform.Messenger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class PlatformListener implements Listener {
    public PlatformListener() {
        Messenger.getInstance().logInfo("BaseListener registered!");
    }

    @EventHandler
    public void onPreJoin(AsyncPlayerPreLoginEvent event) {
        StreamlineUser user = UserUtils.getOrGetUserByName(event.getName());
        if (! (user instanceof StreamlinePlayer player)) return;

        WhitelistConfig whitelistConfig = GivenConfigs.getWhitelistConfig();
        if (whitelistConfig.isEnabled()) {
            WhitelistEntry entry = whitelistConfig.getEntry(player.getUuid());
            if (entry == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Messenger.getInstance().codedString(MainMessagesHandler.MESSAGES.INVALID.WHITELIST_NOT.get()));
                return;
            }
        }

        LoginReceivedEvent loginReceivedEvent = new LoginReceivedEvent(player);
        Streamline.getInstance().fireEvent(loginReceivedEvent, true);

        if (loginReceivedEvent.getResult().isCancelled()) {
            loginReceivedEvent.getResult().validate();

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Messenger.getInstance().codedString(loginReceivedEvent.getResult().getDisconnectMessage()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

//        if (UserManager.getInstance().userExists(player.getUniqueId().toString())) {
//            StreamlinePlayer StreamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
//            StreamlinePlayer.setLatestIP(UserManager.getInstance().parsePlayerIP(player));
//
//            LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(StreamlinePlayer);
//            ModuleUtils.fireEvent(loginCompletedEvent);
//        }
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
//        event.setCancelled(true);

//        if (player.getProtocolVersion().getProtocol() > 759) {
//            if (chatEvent.isCanceled()) {
//                event.setResult(PlayerChatEvent.ChatResult.denied());
////                return;
//            }
//        } else {
//            if (chatEvent.isCanceled()) {
//                event.setResult(PlayerChatEvent.ChatResult.denied());
//                return;
//            }
//            // TODO: Change back once Velocity fixes it.
//            event.setResult(PlayerChatEvent.ChatResult.message(chatEvent.getMessage()));
//            if (event.getResult().getMessage().isPresent()) {
//                String newMessage = event.getResult().getMessage().get();
//                event.setResult(PlayerChatEvent.ChatResult.denied());
//                player.spoofChatInput(newMessage);
//            }
//        }
    }

    @EventHandler
    public void onProperEvent(ProperEvent event) {
        ModuleManager.fireEvent(event.getStreamlineEvent());
    }

    public static class ProxyMessagingListener implements PluginMessageListener {
        public ProxyMessagingListener() {
            Messenger.getInstance().logInfo("Registered " + getClass().getSimpleName() + "!");
        }

        @Override
        public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);

            String subChannel = input.readUTF();

            ProxyMessageIn messageIn = new ProxyMessageIn(channel, subChannel, message);
            ProxyMessageEvent event = new ProxyMessageEvent(messageIn, null);
            ModuleUtils.fireEvent(event);
            if (event.isCancelled()) {
                Messenger.getInstance().logInfo("Cancelled.");
                return;
            }
            SLAPI.getInstance().getProxyMessenger().receiveMessage(event);
        }
    }
}
