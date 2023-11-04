package net.streamline.api.savables.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.StreamlineUser;

@Getter
public class UserNameUpdateEvent extends StreamlineUserEvent<StreamlineUser> {
    @Setter
    private String changeTo;
    private final String changeFrom;

    public UserNameUpdateEvent(StreamlineUser user, String changeTo, String changeFrom) {
        super(user);
        this.changeTo = changeTo;
        this.changeFrom = changeFrom;
    }
}
