package net.streamline.api.interfaces;

import lombok.Getter;
import net.streamline.api.events.StreamlineEvent;

public interface IProperEvent<E> {
    E getEvent();

    StreamlineEvent getStreamlineEvent();

    void setEvent(E event);

    void setStreamlineEvent(StreamlineEvent streamlineEvent);
}
