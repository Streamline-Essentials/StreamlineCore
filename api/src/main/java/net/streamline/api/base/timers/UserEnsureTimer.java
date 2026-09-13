package net.streamline.api.base.timers;

import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

/**
 * A periodic timer that ensures all currently loaded users remain properly
 * initialized. Runs every minute after an initial 30-second delay to catch
 * users that may have partially loaded or whose state has become inconsistent.
 */
public class UserEnsureTimer extends BaseRunnable {

    /**
     * Constructs a new {@code UserEnsureTimer} with a 30-second initial delay
     * and a 1-minute repeat period (expressed in server ticks at 20 ticks/s).
     */
    public UserEnsureTimer() {
        super(20 * 30 // 30 seconds
                , 20 * 60); // 1 minute
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to {@link UserUtils#ensureLoadedUsers()} to verify and repair
     * the state of every user currently held in memory.
     */
    @Override
    public void run() {
        UserUtils.ensureLoadedUsers();
    }
}
