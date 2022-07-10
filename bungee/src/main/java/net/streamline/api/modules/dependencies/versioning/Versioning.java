package net.streamline.api.modules.dependencies.versioning;

import lombok.Getter;

public class Versioning {
    public enum Type {
        EQUAL_TO,
        NOT_EQUAL_TO,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL_TO,
        LESS_THAN,
        LESS_THAN_OR_EQUAL_TO,
        ;
    }

    @Getter
    private final int versioning;
    @Getter
    private final Type type;

    public Versioning(int versioning, Type type) {
        this.versioning = versioning;
        this.type = type;
    }
}
