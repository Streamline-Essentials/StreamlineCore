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
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import net.streamline.api.SLAPI;
import net.streamline.api.events.server.LogoutEvent;
import net.streamline.api.messages.ProxyMessageEvent;
import net.streamline.api.messages.ProxyMessageIn;
import net.streamline.api.messages.ResourcePackMessageBuilder;
import net.streamline.api.messages.SavablePlayerMessageBuilder;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.events.server.LoginReceivedEvent;
import net.streamline.api.events.server.LoginCompletedEvent;
import net.streamline.api.events.server.StreamlineChatEvent;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.scheduler.ModuleRunnable;
import net.streamline.platform.Messenger;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.savables.UserManager;
import net.streamline.platform.users.SavablePlayer;
import net.streamline.platform.users.SavableUser;

public class PlatformListener {
    public PlatformListener() {
        Messenger.getInstance().logInfo("BaseListener registered!");
    }

    @Subscribe
    public void onPreJoin(PreLoginEvent event) {
        InboundConnection connection = event.getConnection();

        StreamlineUser user = UserManager.getInstance().getOrGetUserByName(event.getUsername());
        if (! (user instanceof SavablePlayer player)) return;

        LoginReceivedEvent loginReceivedEvent = new LoginReceivedEvent(player);
        ModuleUtils.fireEvent(loginReceivedEvent);

        if (loginReceivedEvent.getResult().isCancelled()) {
            loginReceivedEvent.getResult().validate();

            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Messenger.getInstance().codedText(loginReceivedEvent.getResult().getDisconnectMessage())));
        }
    }

    @Subscribe
    public void onJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        StreamlinePlayer streamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
        streamlinePlayer.setLatestIP(UserManager.getInstance().parsePlayerIP(player));
        streamlinePlayer.setLatestName(event.getPlayer().getUsername());

        LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(streamlinePlayer);
        ModuleUtils.fireEvent(loginCompletedEvent);
    }

    @Subscribe
    public void onLeave(DisconnectEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        StreamlinePlayer StreamlinePlayer = UserManager.getInstance().getOrGetPlayer(uuid);
        if (StreamlinePlayer == null) return;

        LogoutEvent logoutEvent = new LogoutEvent(StreamlinePlayer);
        ModuleUtils.fireEvent(logoutEvent);

        StreamlinePlayer.getStorageResource().sync();
        UserManager.getInstance().unloadUser(StreamlinePlayer);
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        StreamlinePlayer savablePlayer = UserManager.getInstance().getOrGetPlayer(player);
        savablePlayer.setLatestServer(event.getServer().getServerInfo().getName());

        new BaseRunnable(20, 1) {
            @Override
            public void run() {
                SLAPI.getInstance().getProxyMessenger().sendMessage(SavablePlayerMessageBuilder.build(savablePlayer));
                this.cancel();
            }
        };
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();

        StreamlinePlayer StreamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
        StreamlineChatEvent chatEvent = new StreamlineChatEvent(StreamlinePlayer, event.getMessage());
        ModuleManager.fireEvent(chatEvent);

//        if (chatEvent.isCanceled()) {
//
//            event.setResult(PlayerChatEvent.ChatResult.denied());
////            return;
//        }

        if (player.getProtocolVersion().getProtocol() > 759) {
            if (chatEvent.isCanceled()) {
                event.setResult(PlayerChatEvent.ChatResult.denied());
//                return;
            }
        } else {
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
    }

    @Subscribe
    public void onProperEvent(ProperEvent event) {
        ModuleManager.fireEvent(event.getStreamlineEvent());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        String tag = event.getIdentifier().getId();

        ByteArrayDataInput input = ByteStreams.newDataInput(event.getData());
        String subChannel = input.readUTF();

        ProxyMessageIn messageIn = new ProxyMessageIn(tag, subChannel, event.getData());
        ProxyMessageEvent e = new ProxyMessageEvent(messageIn, null);
        ModuleUtils.fireEvent(e);
        if (e.isCancelled()) return;
        SLAPI.getInstance().getProxyMessenger().receiveMessage(e);
    }
}
