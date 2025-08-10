package net.streamline.api.base.timers;

import lombok.Getter;
import lombok.Setter;
import singularity.configs.given.GivenConfigs;
import singularity.data.teleportation.TPTicket;
import singularity.redis.RedisClient;
import singularity.utils.MessageUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractPlayerTeleporter extends Thread {
    public static final long TICKING_FREQUENCY = 50L; // Milliseconds

    @Getter @Setter
    private static AbstractPlayerTeleporter instance;

    @Getter @Setter
    private static AtomicReference<TeleportStage> stage = new AtomicReference<>(TeleportStage.READY);

    @Getter @Setter
    private static AtomicLong lastRun = new AtomicLong(0);

    public enum TeleportStage {
        COLLECTION,
        TELEPORTATION,
        READY;
    }

    /**
     * Start the teleporter instance.
     */
    public static synchronized void startInstance() {
        if (instance == null) {
            throw new IllegalStateException("Teleporter instance is not set.");
        }
        if (! instance.isAlive()) {
            instance.start();
        }
    }

    /**
     * Stop the teleporter instance safely.
     */
    public static synchronized void stopInstance() {
        if (instance != null && instance.isAlive()) {
            instance.interrupt();
            try {
                instance.join(); // Wait for the thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status
            } finally {
                instance = null;
            }
        }
    }

    /**
     * Check if the teleporter can tick again based on the last run time.
     */
    public static boolean isAbleToRunAgain() {
        long currentTime = System.currentTimeMillis();
        long lastRunTime = lastRun.get();

        if (lastRunTime == 0 || (lastRunTime + TICKING_FREQUENCY < currentTime)) {
            lastRun.set(currentTime);
            return true;
        }
        return false;
    }

    /**
     * Abstract constructor with a custom thread name.
     */
    public AbstractPlayerTeleporter() {
        super("SL - Player Teleporter");
    }

    /**
     * Main run loop for the teleporter thread.
     */
    @Override
    public void run() {
        while (! isInterrupted()) {
            try {
                tick();
            } catch (Throwable e) {
                MessageUtils.logWarning("Error during teleporter tick: ", e);
            }
            try {
                Thread.sleep(TICKING_FREQUENCY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Preserve interrupt status
                break; // Exit the loop
            }
        }
    }

    @Getter @Setter
    private static AtomicReference<CompletableFuture<ConcurrentSkipListSet<TPTicket>>> atomicTicketsPending = new AtomicReference<>(null);

    public CompletableFuture<ConcurrentSkipListSet<TPTicket>> getTicketsPending() {
        if (! isUseRedis()) {
            if (GivenConfigs.getMainDatabase() == null) {
                return CompletableFuture.completedFuture(new ConcurrentSkipListSet<>());
            }

            if (getAtomicTicketsPending().get() == null) {
                getAtomicTicketsPending().set(GivenConfigs.getMainDatabase().pullAllTPTickets());
            }

            return getAtomicTicketsPending().get();
        } else {
            return CompletableFuture.completedFuture(TPTicket.getTickets());
        }
    }

    public void unpendTickets() {
        if (! isUseRedis()) {
            getAtomicTicketsPending().set(null);
        } else {
            TPTicket.getTickets().clear();
        }
    }

    public boolean areTicketsPending() {
        if (! isUseRedis()) {
            return getAtomicTicketsPending().get() != null;
        } else {
            return ! TPTicket.getTickets().isEmpty();
        }
    }

    /**
     * Perform a single tick action if the teleporter is ready.
     */
    public void contemplateTick() {
        if (isAbleToRunAgain()) {
            tick();
        }
    }

    /**
     * Abstract tick method to be implemented by subclasses.
     */
    public abstract void tick();

    /**
     * Clear the ticket and log the action.
     *
     * @param ticket   The teleportation ticket to clear.
     * @param instance The instance ID for logging.
     */
    private static void clearTicket(TPTicket ticket, int instance) {
        if (ticket != null) {
            ticket.clear();
            MessageUtils.logInfo("Cleared teleportation ticket for player " + ticket.getIdentifier() + ". [" + instance + "]");
        }
    }

    /**
     * Check if Redis is being used for teleportation.
     *
     * @return true if Redis is connected, false otherwise.
     */
    public static boolean isUseRedis() {
        return RedisClient.isConnected();
    }
}
