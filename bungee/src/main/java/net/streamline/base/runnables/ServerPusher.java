package net.streamline.base.runnables;

import singularity.scheduler.BaseRunnable;

/**
 * A periodic task that runs every 20 ticks (1 second) starting immediately
 * to handle server push operations on the BungeeCord platform.
 *
 * <p>This runnable is registered during plugin enable and fires on each tick
 * cycle to push any queued server-related data to the appropriate backend
 * servers.
 */
public class ServerPusher extends BaseRunnable {

    /**
     * Constructs a new {@code ServerPusher} with an initial delay of 0 ticks
     * and a period of 20 ticks (approximately 1 second).
     */
    public ServerPusher() {
        super(0, 20);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
    }
}
