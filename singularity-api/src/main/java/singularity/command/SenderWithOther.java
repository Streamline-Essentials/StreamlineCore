package singularity.command;

/**
 * Indicates whether the sender is the "from" or "to" party in a command context.
 */
public enum SenderWithOther {
    /**
     * The "from" party is treated as the primary sender, and the "to" party is the
     * other participant whose placeholders are substituted.
     */
    FROM_IS_SENDER,

    /**
     * The "to" party is treated as the primary sender, and the "from" party is the
     * other participant whose placeholders are substituted.
     */
    TO_IS_SENDER,
    ;
}