package net.streamline.platform.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.streamline.api.SLAPI;
import net.streamline.api.events.server.LogoutEvent;
import net.streamline.api.messages.*;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.events.server.LoginReceivedEvent;
import net.streamline.api.events.server.LoginCompletedEvent;
import net.streamline.api.events.server.StreamlineChatEvent;
import net.streamline.base.Streamline;
import net.streamline.platform.Messenger;
import net.streamline.platform.events.ProperEvent;
import net.streamline.platform.savables.UserManager;
import net.streamline.platform.users.SavablePlayer;

public class PlatformListener implements Listener {
    public PlatformListener() {
        Messenger.getInstance().logInfo("BaseListener registered!");
    }

    @EventHandler
    public void onPreJoin(PreLoginEvent event) {
        PendingConnection connection = event.getConnection();

        StreamlineUser user = SLAPI.getInstance().getUserManager().getOrGetUserByName(connection.getName());
        if (! (user instanceof StreamlinePlayer player)) return;

        LoginReceivedEvent loginReceivedEvent = new LoginReceivedEvent(player);
        ModuleUtils.fireEvent(loginReceivedEvent);

        if (loginReceivedEvent.getResult().isCancelled()) {
            loginReceivedEvent.getResult().validate();

            event.setCancelReason(Messenger.getInstance().codedText(loginReceivedEvent.getResult().getDisconnectMessage()));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        StreamlinePlayer StreamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
        StreamlinePlayer.setLatestIP(UserManager.getInstance().parsePlayerIP(player));

        LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(StreamlinePlayer);
        ModuleUtils.fireEvent(loginCompletedEvent);
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        StreamlinePlayer StreamlinePlayer = UserManager.getInstance().getOrGetPlayer(uuid);
        if (StreamlinePlayer == null) return;

        LogoutEvent logoutEvent = new LogoutEvent(StreamlinePlayer);
        ModuleUtils.fireEvent(logoutEvent);

        StreamlinePlayer.getStorageResource().sync();
        UserManager.getInstance().unloadUser(StreamlinePlayer);
    }

    @EventHandler
    public void onServerSwitch(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();

        StreamlinePlayer savablePlayer = UserManager.getInstance().getOrGetPlayer(player);
        savablePlayer.setLatestServer(event.getServer().getInfo().getName());

        SLAPI.getInstance().getProxyMessenger().sendMessage(SavablePlayerMessageBuilder.build(savablePlayer));
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
        String newMessage = event.getMessage();
        event.setCancelled(true);
        player.chat(newMessage);

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

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getReceiver() instanceof ProxiedPlayer) return;

        String tag = event.getTag();
        if (event.getData() == null) return;

        try {
            ByteArrayDataInput input = ByteStreams.newDataInput(event.getData());
            String subChannel = input.readUTF();

            ProxyMessageIn messageIn = new ProxyMessageIn(tag, subChannel, event.getData());
            ProxyMessageEvent e = new ProxyMessageEvent(messageIn, null);
            ModuleUtils.fireEvent(e);
            if (e.isCancelled()) return;
            SLAPI.getInstance().getProxyMessenger().receiveMessage(e);
        } catch (Exception e) {
            // do nothing.
        }
    }
}
