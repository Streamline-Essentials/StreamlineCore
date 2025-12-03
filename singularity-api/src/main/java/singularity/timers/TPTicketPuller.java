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

public class TPTicketPuller extends BaseRunnable {
    @Getter @Setter
    private static AtomicBoolean running = new AtomicBoolean(false);

    public TPTicketPuller() {
        super(0, 5);

        MessageUtils.logInfo("Registered &cTPTicket Puller&r...");
    }

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

    public static boolean isUseRedis() {
        return GivenConfigs.getRedisConfig().isEnabled() && OwnRedisClient.isConnected();
    }
}
