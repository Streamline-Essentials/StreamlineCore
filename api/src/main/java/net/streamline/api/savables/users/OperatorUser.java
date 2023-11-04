package net.streamline.api.savables.users;

import lombok.Getter;
import lombok.Setter;

@Getter
public class OperatorUser {
    @Setter
    private StreamlineUser parent;

    public OperatorUser(StreamlineUser parent) {
        this.parent = parent;
    }
}
