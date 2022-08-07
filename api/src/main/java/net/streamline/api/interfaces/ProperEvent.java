package net.streamline.api.interfaces;

import lombok.Getter;

public abstract class ProperEvent<E> {
    @Getter
    private final E event;

    public ProperEvent(E event) {
        this.event = event;
    }
}
