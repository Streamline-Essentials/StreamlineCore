package net.streamline.api.interfaces;

import net.streamline.api.events.StreamlineEvent;

public interface IProperEvent<E> {
    E getEvent();

    StreamlineEvent getStreamlineEvent();

    void setEvent(E event);

    void setStreamlineEvent(StreamlineEvent streamlineEvent);
}
