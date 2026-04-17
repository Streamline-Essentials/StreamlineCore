package singularity.interfaces.audiences.messaging;

/**
 * Represents an entity capable of writing output to the server console.
 *
 * <p>Two distinct output channels are exposed: a platform-native console channel
 * ({@link #sendConsoleMessageNonNull}) that formats messages through the platform's
 * own logging pipeline, and a raw log channel ({@link #sendLogMessage}) used as a
 * fallback when the native console is unavailable or throws.</p>
 */
public interface IConsolable {

    /**
     * Sends a message directly to the platform's console output.
     *
     * <p>Implementations must guarantee that the underlying console reference is
     * non-{@code null} before writing (hence the method name). Callers in
     * {@link singularity.interfaces.audiences.IConsoleHolder} rely on this contract
     * and handle any exception by falling back to {@link #sendLogMessage}.</p>
     *
     * @param message the message text to write to the platform console
     */
    void sendConsoleMessageNonNull(String message);

    /**
     * Writes a message through the framework's own logging mechanism.
     *
     * <p>Used as a fallback when the native platform console is unavailable.  Colour
     * codes should already be stripped before this method is called, as most loggers
     * do not understand Minecraft formatting characters.</p>
     *
     * @param message the plain-text message to write to the log
     */
    void sendLogMessage(String message);
}
