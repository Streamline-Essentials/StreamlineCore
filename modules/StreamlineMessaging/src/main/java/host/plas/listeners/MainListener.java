package host.plas.listeners;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseEventPriority;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.database.MyLoader;
import host.plas.utils.ChatHandler;
import singularity.events.server.LoginCompletedEvent;
import singularity.events.server.CosmicChatEvent;
import host.plas.StreamlineMessaging;
import host.plas.configs.ConfiguredChatChannel;
import host.plas.savables.SavableChatter;
import singularity.modules.ModuleUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainListener implements BaseEventListener {
    public MainListener() {
        ModuleUtils.listen(this, StreamlineMessaging.getInstance());

        StreamlineMessaging.getInstance().logInfo("Initialized listener: &c" + this.getClass().getSimpleName());
    }

    @BaseProcessor(priority = BaseEventPriority.LOWEST)
    public void onChat(CosmicChatEvent event) {
        if (event.isCanceled()) return;

        AtomicBoolean handled = new AtomicBoolean(false);
        Optional<ConfiguredChatChannel> defaultChat = ChatHandler.getDefaultChannel();
        if (defaultChat.isPresent()) {
            ConfiguredChatChannel cc = defaultChat.get();

            cc.sendMessageAs(event.getSender(), event.getMessage());

            handled.set(true);
            event.setCanceled(true);
            return;
        }

        StreamlineMessaging.getChatChannelConfig().getChatChannels().forEach((s, chatChannel) -> {
            if (! event.getSender().hasPermission(chatChannel.getAccessPermission())) return;
            if (chatChannel.getIdentifier().equals("none")) return;
            if (handled.get()) return;
            if (chatChannel.getPrefix().isEmpty()) return;
            if (! event.getMessage().startsWith(chatChannel.getPrefix())) return;

            String message = event.getMessage().substring(chatChannel.getPrefix().length());

            chatChannel.sendMessageAs(event.getSender(), message);
            handled.set(true);
        });
        if (handled.get()) {
            event.setCanceled(true);
            return;
        }

        SavableChatter chatter = MyLoader.getInstance().getOrCreate(event.getSender().getUuid());
        if (chatter == null) return;
        event.setCanceled(chatter.onChannelMessage(event));
    }

    @BaseProcessor
    public void onJoin(LoginCompletedEvent event) {
        SavableChatter chatter = MyLoader.getInstance().getOrCreate(event.getSender().getUuid());
        if (chatter == null) return;

        if (StreamlineMessaging.getConfigs().forceDefaultOnJoin()) {
            ConfiguredChatChannel channel = StreamlineMessaging.getChatChannelConfig().getChatChannel(StreamlineMessaging.getConfigs().defaultChat());
            if (channel == null) return;
            chatter.setCurrentChatChannel(channel);
        }
    }
}
