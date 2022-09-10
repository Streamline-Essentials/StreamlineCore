package net.streamline.api.messages;

import lombok.Getter;
import net.streamline.api.events.EventProcessor;
import net.streamline.api.messages.builders.ProxyParseMessageBuilder;
import net.streamline.api.messages.builders.ReturnParseMessageBuilder;
import net.streamline.api.savables.users.StreamlineUser;

public class ReturnableQuery extends ReturnableMessage {
    @Getter
    private final String toQuery;
    @Getter
    private final StreamlineUser user;

    public ReturnableQuery(String query, StreamlineUser user) {
        super(ProxyParseMessageBuilder.build(query, user), ReturnParseMessageBuilder.getSubChannel(), 1);
        this.toQuery = query;
        this.user = user;
    }

    @EventProcessor
    @Override
    public void onProxyMessage(ProxyMessageEvent event) {
        super.onProxyMessage(event);
        ProxyMessageHelper.cacheQuery(getToQuery(), getQuery());
    }

    public String getQuery() {
        return getReturned()[0];
    }
}
