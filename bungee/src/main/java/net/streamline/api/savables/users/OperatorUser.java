package net.streamline.api.savables.users;

import lombok.Getter;

public class OperatorUser {
    @Getter
    private final SavableUser parent;

    public OperatorUser(SavableUser parent) {
        this.parent = parent;
    }
}
