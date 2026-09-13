package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import lombok.Setter;
import host.plas.StreamlineMessaging;

@Getter @Setter
public class Configs extends SimpleConfiguration {
    public Configs() {
        super("config.yml", StreamlineMessaging.getInstance().getDataFolder(), true);
    }

    @Override
    public void init() {
        if (getResource().contains("chatters.saving.databases")) {
            getResource().remove("chatters.saving.databases");
        }

        defaultChat();
        forceDefaultAlways();
        forceDefaultOnJoin();

        messagingMessagePermissionFormatting();
        messagingMessageUpdateSender();
        messagingMessageUpdateRecipient();

        messagingReplyPermissionFormatting();
        messagingReplyUpdateSender();
        messagingReplyUpdateRecipient();
    }

    /*
    ===========================================================================

                                     CHANNELS

    ===========================================================================
     */

    public String defaultChat() {
        reloadResource();

        return getResource().getOrSetDefault("chat-channels.default", "none");
    }

    public boolean forceDefaultAlways() {
        reloadResource();

        return getResource().getOrSetDefault("chat-channels.force-default.always", false);
    }

    public boolean forceDefaultOnJoin() {
        reloadResource();

        return getResource().getOrSetDefault("chat-channels.force-default.on-join", true);
    }

    /*
    ===========================================================================

                                     MESSAGING

    ===========================================================================
     */

    public String messagingMessagePermissionFormatting() {
        reloadResource();

        return getResource().getOrSetDefault("messaging.message.permissions.formatting", "streamline.messaging.formatting.message");
    }

    public boolean messagingMessageUpdateSender() {
        reloadResource();

        return getResource().getOrSetDefault("messaging.message.update-reply-to.sender", true);
    }

    public boolean messagingMessageUpdateRecipient() {
        reloadResource();

        return getResource().getOrSetDefault("messaging.message.update-reply-to.recipient", true);
    }

    public String messagingReplyPermissionFormatting() {
        reloadResource();

        return getResource().getOrSetDefault("messaging.reply.permissions.formatting", "streamline.messaging.formatting.message");
    }

    public boolean messagingReplyUpdateSender() {
        reloadResource();

        return getResource().getOrSetDefault("messaging.reply.update-reply-to.sender", true);
    }

    public boolean messagingReplyUpdateRecipient() {
        reloadResource();

        return getResource().getOrSetDefault("messaging.reply.update-reply-to.recipient", true);
    }


    /*
    ===========================================================================

                                     CHATTERS

    ===========================================================================
     */

    // FRIENDS

    public int friendInviteTime() {
        reloadResource();

        return getResource().getOrSetDefault("friends.invites.timeout", 600);
    }
}
