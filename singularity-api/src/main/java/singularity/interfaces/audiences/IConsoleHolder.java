package singularity.interfaces.audiences;

import singularity.Singularity;
import singularity.interfaces.audiences.real.RealSender;

/**
 * Contract for platform-specific holders of the server console sender.
 *
 * <p>Provides a unified API for obtaining the console object and dispatching messages
 * to it, abstracting over the differences between Velocity, BungeeCord, and Spigot consoles.
 * If the underlying platform console is unavailable, messages fall back to the framework's
 * own logging mechanism via {@link RealSender#sendLogMessage}.</p>
 *
 * @param <C> the platform command-sender type that represents the console on this platform
 */
public interface IConsoleHolder<C> {

    /**
     * Returns the {@link RealSender} wrapper around the platform console object.
     *
     * @return the real-sender representation of the console; never {@code null}
     */
    RealSender<C> getRealConsole();

    /**
     * Returns the raw platform console sender object.
     *
     * <p>Delegates to {@link #getRealConsole()}{@code .getConsole()}.  May return
     * {@code null} on platforms where the console is not yet initialised.</p>
     *
     * @return the native platform console, or {@code null} if unavailable
     */
    default C getConsole() {
        return getRealConsole().getConsole();
    }

    /**
     * Sends a formatted message to the server console.
     *
     * <p>If the native console is available, delegates to
     * {@link RealSender#sendConsoleMessageNonNull}.  If that call throws, or if the
     * native console is {@code null}, the message is stripped of colour codes and written
     * via {@link RealSender#sendLogMessage} instead.</p>
     *
     * @param message the message text (may contain colour codes) to send to the console
     */
    default void sendConsoleMessage(String message) {
        if (getConsole() != null) {
            try {
                getRealConsole().sendConsoleMessageNonNull(message);
            } catch (Exception e) {
                getRealConsole().sendLogMessage(Singularity.getInstance().getMessenger().stripColor(message));
            }
        } else {
            getRealConsole().sendLogMessage(Singularity.getInstance().getMessenger().stripColor(message));
        }
    }
}
