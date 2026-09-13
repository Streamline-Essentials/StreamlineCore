package singularity.timers;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.teleportation.TPTicket;
import singularity.redis.OwnRedisClient;
import singularity.scheduler.BaseRunnable;
import singularity.utils.MessageUtils;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A recurring scheduler task that pulls unprocessed {@link TPTicket}s from the
 * database every 5 ticks and registers them as pending for the {@link TPTicketFlusher}.
 *
 * <p>The puller is a no-op when Redis is active (detected via {@link #isUseRedis()})
 * because in that case tickets are delivered through the Redis pub/sub channel instead
 * of being polled from the database.  It also skips execution if the database is not
 * yet ready or if a previous pull has not finished.
 */
public class TPTicketPuller extends BaseRunnable {

    /**
     * Guard flag set to {@code true} while a pull is in progress to prevent
     * concurrent or re-entrant execution.
     */
    @Getter @Setter
    private static AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Creates and registers the {@code TPTicketPuller} with a period of 5 ticks
     * and no initial delay.
     */
    public TPTicketPuller() {
        super(0, 5);

        MessageUtils.logInfo("Registered &cTPTicket Puller&r...");
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        if (isUseRedis()) {
            running.set(false);
            return; // No need to pull if using Redis
        }

        if (! Singularity.isDatabaseReady()) {
            running.set(false);
            return;
        }

        if (running.get()) return;
        running.set(true);

        try {
            ConcurrentSkipListSet<TPTicket> set = GivenConfigs.getMainDatabase().pullAllTPTickets().join();
            if (set == null || set.isEmpty()) {
                running.set(false);
                return;
            }

            set.forEach(TPTicket::pend);
        } catch (Throwable e) {
            MessageUtils.logWarning("An error occurred while pulling TPTickets: " + e.getMessage());
            e.printStackTrace();
        }


        running.set(false);
    }

    /**
     * Checks whether Redis is both enabled in configuration and currently connected,
     * which means the puller should skip database polling.
     *
     * @return {@code true} if Redis is active and tickets are delivered via pub/sub
     */
    public static boolean isUseRedis() {
        return GivenConfigs.getRedisConfig().isEnabled() && OwnRedisClient.isConnected();
    }
}
