package net.streamline.base.runnables;

import singularity.scheduler.BaseRunnable;

/**
 * A periodic task intended to push server-state information at a fixed interval
 * (initial delay 200 ticks, period 3600 ticks).
 *
 * <p>The implementation body is currently empty and reserved for future use.
 */
public class ServerPusher extends BaseRunnable {
    /**
     * Constructs a new {@code ServerPusher} scheduled with an initial delay of 200 ticks
     * and a repeat period of 3600 ticks.
     */
    public ServerPusher() {
        super(200, 3600);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Currently a no-op; server-push logic is reserved for a future implementation.
     */
    @Override
    public void run() {

    }
}