package singularity.configs.given.punishments;

/**
 * Represents the category of a player punishment applied by the server.
 */
public enum PunishmentType {
    /** Prevents the player from joining the server for a specified duration. */
    BAN,
    /** Prevents the player from sending chat messages for a specified duration. */
    MUTE,
    /** Removes the player from the server immediately with an optional reason. */
    KICK,
    /** Issues a formal warning to the player without removing server access. */
    WARN,
    ;
}
