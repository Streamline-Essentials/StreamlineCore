package net.streamline.api.base.timers;

import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

/**
 * A repeating task that increments the play-time of every online sender by one
 * second on each tick cycle.
 *
 * <p>The timer fires with an initial delay of 0 ticks and a repeat period of
 * 20 ticks, which corresponds to approximately one real-world second on a
 * normally-loaded Minecraft server. Only senders that are currently marked as
 * online have their play-time incremented.</p>
 */
public class OneSecondTimer extends BaseRunnable {

    /**
     * Constructs the timer with a 0-tick initial delay and a 20-tick (1 second)
     * repeat period.
     */
    public OneSecondTimer() {
        super(0, 20);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates over all loaded senders and adds one play-second to each
     * sender that is currently online.</p>
     */
    @Override
    public void run() {
        UserUtils.getLoadedSenders().forEach((s, user) -> {
            if (! user.isOnline()) return;

            user.addPlaySeconds(1);
        });
    }
}
