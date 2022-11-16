package net.streamline.platform.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.CachedUUIDsHandler;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.configs.given.whitelist.WhitelistConfig;
import net.streamline.api.configs.given.whitelist.WhitelistEntry;
import net.streamline.api.events.server.*;
import net.streamline.api.messages.builders.UserNameMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.messages.builders.SavablePlayerMessageBuilder;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import net.streamline.platform.Messenger;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.savables.UserManager;

public class PlatformListener {
    public PlatformListener() {
        MessageUtils.logInfo("BaseListener registered!");
    }

    @Subscribe
    public void onPreJoin(PreLoginEvent event) {
        InboundConnection connection = event.getConnection();

        StreamlineUser user = UserUtils.getOrGetUserByName(event.getUsername());
        if (! (user instanceof StreamlinePlayer player)) return;

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
        if (! (event.getTarget() instanceof Player player)) return;
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
}
