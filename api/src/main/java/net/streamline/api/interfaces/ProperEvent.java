package net.streamline.api.interfaces;

import lombok.Getter;

@Getter
public abstract class ProperEvent<E> {
    private final E event;

    public ProperEvent(E event) {
        this.event = event;
    }
}
