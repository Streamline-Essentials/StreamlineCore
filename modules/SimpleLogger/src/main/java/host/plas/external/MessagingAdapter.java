package host.plas.external;

import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseEventPriority;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.SimpleLogger;
import host.plas.StreamlineMessaging;
import host.plas.events.ChannelMessageEvent;
import host.plas.managers.WebhookManager;
import lombok.Getter;
import lombok.Setter;
import singularity.holders.ModuleDependencyHolder;

@Getter @Setter
public class MessagingAdapter extends ModuleDependencyHolder<StreamlineMessaging> {
    private MessagingListener listener;

    public MessagingAdapter() {
        super("streamline-messaging");

        try {
            listener = new MessagingListener();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class MessagingListener implements BaseEventListener {
        public MessagingListener() {
            BaseEventHandler.bake(this, SimpleLogger.getInstance());

            SimpleLogger.getInstance().logInfo("Hooked into &cStreamlineMessaging&f!");
        }

        @BaseProcessor(priority = BaseEventPriority.HIGHEST, ignoreCancelled = false)
        public void onMessage(ChannelMessageEvent event) {
            WebhookManager.sendChannelMessage(event.getChatChannel(), event.getSender(), event.getMessage());
        }
    }
}
