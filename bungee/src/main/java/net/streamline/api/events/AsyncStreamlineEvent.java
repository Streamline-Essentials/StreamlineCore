package net.streamline.api.events;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

public class AsyncStreamlineEvent<T> extends AsyncEvent<T> {
    public AsyncStreamlineEvent(Callback<T> done) {
        super(done);
    }
}
