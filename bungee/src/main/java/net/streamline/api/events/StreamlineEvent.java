package net.streamline.api.events;

import net.md_5.bungee.api.plugin.Event;

import java.util.Date;

public abstract class StreamlineEvent extends Event {
    public Date firedAt;

    public StreamlineEvent() {
        this.firedAt = new Date();
    }
}
