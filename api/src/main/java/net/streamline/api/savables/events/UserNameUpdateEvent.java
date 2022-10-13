package net.streamline.api.savables.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.StreamlineUser;

public class UserNameUpdateEvent extends StreamlineUserEvent<StreamlineUser> {
    @Getter @Setter
    private String changeTo;
    @Getter
    private final String changeFrom;

    public UserNameUpdateEvent(StreamlineUser user, String changeTo, String changeFrom) {
        super(user);
        this.changeTo = changeTo;
        this.changeFrom = changeFrom;
    }
}
