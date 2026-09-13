package singularity.configs.given.punishments;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.UUID;

/**
 * Represents a single punishment record applied to a player, including who
 * issued it, the reason, the type, and when the punishment expires.
 *
 * <p>The unique {@code idHash} is derived from the first segment of a randomly
 * generated UUID (upper-cased), giving each punishment a short, human-readable ID.</p>
 */
@Setter
@Getter
public class CosmicPunishment implements Comparable<CosmicPunishment> {

    /** Short upper-cased identifier derived from a random UUID, unique per punishment. */
    private String idHash;

    /** UUID string of the player who received this punishment. */
    private String punishedUUID;

    /** UUID string of the staff member or system that issued this punishment. */
    private String punisherUUID;

    /** Human-readable reason given for this punishment. */
    private String reason;

    /** Category of punishment (ban, mute, kick, or warn). */
    private PunishmentType type;

    /** The point in time at which this punishment expires and should be lifted. */
    private Date timeToUnpunish;

    /**
     * Creates a new {@code CosmicPunishment} and auto-generates a short {@code idHash}
     * from the first segment of a random UUID.
     *
     * @param punishedUUID   UUID string of the player being punished
     * @param punisherUUID   UUID string of the issuer (player or system)
     * @param reason         human-readable reason for the punishment
     * @param type           the {@link PunishmentType} category of this punishment
     * @param timeToUnpunish the date/time at which the punishment expires
     */
    public CosmicPunishment(String punishedUUID, String punisherUUID, String reason, PunishmentType type, Date timeToUnpunish) {
        String uuid = UUID.randomUUID().toString();
        this.idHash = uuid.substring(0, uuid.indexOf("-")).toUpperCase();
        this.punishedUUID = punishedUUID;
        this.punisherUUID = punisherUUID;
        this.reason = reason;
        this.type = type;
        this.timeToUnpunish = timeToUnpunish;
    }

    /**
     * Compares this punishment to another by their {@code idHash} strings,
     * enabling natural ordering in sorted collections.
     *
     * @param o the other {@link CosmicPunishment} to compare against
     * @return a negative, zero, or positive integer as this idHash is
     *         lexicographically less than, equal to, or greater than the other's
     */
    @Override
    public int compareTo(@NotNull CosmicPunishment o) {
        return this.idHash.compareTo(o.idHash);
    }
}
