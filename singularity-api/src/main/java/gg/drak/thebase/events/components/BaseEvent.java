package gg.drak.thebase.events.components;

import lombok.Getter;
import lombok.Setter;
import gg.drak.thebase.events.BaseEventHandler;

import java.util.Date;

public class BaseEvent {
    @Getter
    private final Date firedAt;
    @Getter @Setter
    private boolean completed;
    @Getter @Setter
    private boolean cancelled;

    public BaseEvent() {
        this.firedAt = new Date();
        this.completed = false;
        this.cancelled = false;
    }

    public String getEventName() {
        return getClass().getSimpleName();
    }

    public <T extends BaseEvent> T fire() {
        try {
            T t = (T) this;
        } catch (Exception e) {
            return null;
        }

        BaseEventHandler.fireEvent(this);
        return (T) this;
    }
}
