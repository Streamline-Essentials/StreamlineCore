package singularity.database.servers;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * Carries the timestamp and originating server UUID for a database update record.
 *
 * <p>Instances are {@link Comparable} so they can be ordered chronologically by
 * their {@link #date} field.</p>
 */
@Getter @Setter
public class UpdateInfo implements Comparable<UpdateInfo> {

    /**
     * The date and time at which the update was posted.
     */
    private Date date;

    /**
     * The UUID of the server that posted the update.
     */
    private String serverUuid;

    /**
     * Constructs an {@code UpdateInfo} with the given date and server UUID.
     *
     * @param date       the timestamp of the update
     * @param serverUuid the UUID of the server that posted the update
     */
    public UpdateInfo(Date date, String serverUuid) {
        setDate(date);
        setServerUuid(serverUuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ordering is determined solely by the {@link #date} field.</p>
     */
    @Override
    public int compareTo(@NotNull UpdateInfo o) {
        return getDate().compareTo(o.getDate());
    }
}
