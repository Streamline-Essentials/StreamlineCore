package net.streamline.api.base.timers;

import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

/**
 * A periodic timer that synchronizes all loaded users with persistent storage
 * every 3 minutes. Starts immediately on construction (no initial delay) so
 * that the first sync happens as soon as the plugin is ready.
 */
public class UserSyncTimer extends BaseRunnable {

    /**
     * Constructs a new {@code UserSyncTimer} with no initial delay and a
     * 3-minute repeat period (expressed in server ticks at 20 ticks/s).
     */
    public UserSyncTimer() {
        super(0, 20 * 60 * 3); // 3 minutes
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to {@link UserUtils#syncAllUsers()} to persist the current
     * state of every loaded user to the configured storage back-end.
     */
    @Override
    public void run() {
        UserUtils.syncAllUsers();
    }
}
