package singularity.logging;

/**
 * Represents the semantic intent (severity level) of a log entry within the
 * Singularity logging system.
 *
 * <p>Platform-specific log levels from both Java Util Logging (JUL) and Logback
 * are normalised to one of these values before being broadcast as a
 * {@link singularity.events.server.ServerLogTextEvent}.</p>
 */
public enum LogIntent {

    /** Informational message indicating normal operation. */
    INFO,

    /** Non-critical warning that may require attention. */
    WARNING,

    /** Serious error that may affect server stability or correctness. */
    SEVERE,

    /** Detailed diagnostic message typically only enabled during development. */
    DEBUG,

    /**
     * Catch-all for log levels that do not map directly to any of the defined
     * intents above.
     */
    OTHER,
    ;
}