package singularity.database;

import lombok.Getter;
import lombok.Setter;
import singularity.scheduler.BaseDelayedRunnable;

import java.util.Date;

/**
 * A scheduled task that tracks the start time of a database connection acquisition
 * attempt.
 *
 * <p>{@code ConnectionTimer} extends {@link BaseDelayedRunnable} with a fixed delay
 * of 20 seconds (20 ticks &times; 20 = 400 ticks at 20 TPS). The original intent
 * was to close and evict stale connections from the connection map after the delay
 * elapsed; that logic is currently commented out pending a future implementation.</p>
 */
@Getter @Setter
public class ConnectionTimer extends BaseDelayedRunnable {

    /**
     * The timestamp at which the associated connection request was initiated.
     * Used to correlate this timer with a specific connection in the connection map.
     */
    private Date qStart;

    /**
     * Creates a new {@code ConnectionTimer} for the given connection-start timestamp.
     * The timer fires after 20 seconds (400 ticks at 20 TPS).
     *
     * @param qStart the {@link Date} at which the connection request began
     */
    public ConnectionTimer(Date qStart) {
        super(20 * 20); // 20 seconds

        this.qStart = qStart;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Currently a no-op. The original implementation closed and removed the
     * connection associated with {@link #qStart} from the connection map; that
     * logic has been disabled pending further review.</p>
     */
    @Override
    public void runDelayed() {
//        Connection connection = SLAPI.getMainDatabase().getConnectionMap().get(getQStart());
//        if (connection == null) return;
//
//        try {
//            if (! connection.isClosed()) connection.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        SLAPI.getMainDatabase().getConnectionMap().remove(getQStart());
//        SLAPI.getMainDatabase().getConnectionTimers().remove(getQStart());
    }
}
