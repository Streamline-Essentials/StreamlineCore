package net.streamline.api.savables.users;

import lombok.Getter;
import lombok.Setter;

public class OperatorUser {
    @Getter @Setter
    private StreamlineUser parent;

    public OperatorUser(StreamlineUser parent) {
        this.parent = parent;
    }
}
