package net.streamline.api.modules.dependencies.versioning;

import lombok.Getter;

public record Versioning(@Getter int versioning, @Getter Type type) {
    public enum Type {
        EQUAL_TO,
        NOT_EQUAL_TO,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL_TO,
        LESS_THAN,
        LESS_THAN_OR_EQUAL_TO,
        ;
    }

}
