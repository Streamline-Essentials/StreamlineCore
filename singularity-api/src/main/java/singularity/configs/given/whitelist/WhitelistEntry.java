package singularity.configs.given.whitelist;

import singularity.configs.given.GivenConfigs;

import java.util.Date;

/**
 * Immutable record of a single whitelist entry, capturing which player was
 * whitelisted, when they were added, and who added them.
 */
public class WhitelistEntry {

    /** UUID of the player who has been granted whitelist access. */
    final String whitelistedUuid;

    /** The timestamp at which this whitelist entry was created. */
    final Date whitelistedAt;

    /**
     * The discriminator or UUID of the person or system that added this entry.
     * May be the console discriminator if added via console.
     */
    final String whitelistedBy;

    /**
     * Creates a new whitelist entry with the given player UUID, creation
     * timestamp, and the identifier of whoever granted the access.
     *
     * @param whitelistedUuid UUID of the player being whitelisted
     * @param whitelistedAt   timestamp at which the entry was created
     * @param whitelistedBy   discriminator/UUID of the granting party
     */
    public WhitelistEntry(String whitelistedUuid, Date whitelistedAt, String whitelistedBy) {
        this.whitelistedUuid = whitelistedUuid;
        this.whitelistedAt = whitelistedAt;
        this.whitelistedBy = whitelistedBy;
    }

    /**
     * Returns the UUID of the whitelisted player.
     *
     * @return the player UUID string
     */
    public String whitelistedUuid() {
        return whitelistedUuid;
    }

    /**
     * Returns the date and time at which this entry was created.
     *
     * @return the creation timestamp
     */
    public Date whitelistedAt() {
        return whitelistedAt;
    }

    /**
     * Returns the identifier of the party that granted this whitelist entry.
     *
     * @return the granting party's discriminator or UUID string
     */
    public String whitelistedBy() {
        return whitelistedBy;
    }

    /**
     * Adds this entry to the global whitelist configuration.
     */
    public void add() {
        GivenConfigs.getWhitelistConfig().addEntry(this);
    }

    /**
     * Removes this entry from the global whitelist configuration.
     */
    public void remove() {
        GivenConfigs.getWhitelistConfig().removeEntry(this);
    }

    /**
     * Returns whether this entry is currently present in the whitelist.
     *
     * @return {@code true} if the player's UUID is found in the active whitelist;
     *         {@code false} otherwise
     */
    public boolean applied() {
        return GivenConfigs.getWhitelistConfig().isEntryApplied(whitelistedUuid());
    }
}
