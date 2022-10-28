package net.streamline.api.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.modules.ModuleManager;
import tv.quaint.events.components.BaseEvent;

import java.util.Date;

public abstract class StreamlineEvent extends BaseEvent {
    public StreamlineEvent() {}
}
