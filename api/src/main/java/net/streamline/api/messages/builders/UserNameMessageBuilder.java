package net.streamline.api.messages.builders;

import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.answered.ReturnableMessage;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.events.UserNameUpdateByOtherEvent;
import net.streamline.api.savables.events.UserNameUpdateEvent;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;

public class UserNameMessageBuilder {
    @Getter
    private static final String subChannel = "name-message";

    public static ProxiedMessage build(StreamlinePlayer carrier, String name, StreamlineUser user) {
        ProxiedMessage r = new ProxiedMessage(carrier, SLAPI.isProxy());

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", user.getUuid());
        r.write("name", name);

        return r;
    }

    public static void handle(ProxiedMessage in) {
        if (! in.getSubChannel().equals(getSubChannel())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + ServerConnectMessageBuilder.class.getSimpleName() + "'.");
            return;
        }

        String uuid = in.getString("user_uuid");
        String name = in.getString("name");

        StreamlineUser user = UserUtils.getOrGetUser(uuid);
        if (user == null) {
            MessageUtils.logWarning("Tried to get a user with uuid of '" + uuid + "', but found none!");
            return;
        }

        UserNameUpdateByOtherEvent event = new UserNameUpdateByOtherEvent(user, name, user.getDisplayName(), in);
        event = event.fire();
        if (event.isCancelled()) return;

        user.setDisplayName(event.getChangeTo());
    }
}
