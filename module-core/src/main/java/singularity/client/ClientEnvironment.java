package singularity.client;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Where the game client keeps Streamline's client files, and where client code logs to.
 *
 * <p>The mod-loader client sets both before anything client-side runs. Everywhere else the
 * folder stays unset, so the HUD layout and module settings are kept in memory only and a
 * server never writes client files.</p>
 *
 * <p>Layout under {@link #folder()} ({@code <Streamline data folder>/client}):</p>
 * <ul>
 *     <li>{@code modules/} the client module jars</li>
 *     <li>{@code module-data/<module-id>/} each client module's own files</li>
 *     <li>{@code settings/<module-id>.properties} values from the client menu</li>
 *     <li>{@code client-settings.properties} StreamlineClient's own settings (Client Settings)</li>
 *     <li>{@code hud-layout.properties} where the player placed each HUD element</li>
 *     <li>{@code modules.properties} which modules the player turned off</li>
 *     <li>{@code slclient-latest.log} this session's StreamlineClient log, and
 *         {@code slclient-<yyyy-MM-dd_HH-mm-ss>.log.gz} earlier sessions' (see {@link ClientLogFile})</li>
 * </ul>
 */
public final class ClientEnvironment {

    private static volatile File folder;

    private static volatile BiConsumer<Level, String> log = (level, message) ->
            Logger.getLogger("StreamlineClient").log(level, message);

    private ClientEnvironment() {}

    /** @return the client folder, or {@code null} off a game client */
    public static File folder() {
        return folder;
    }

    /** @return a file or folder inside the client folder, or {@code null} off a game client */
    public static File file(String... path) {
        File current = folder;
        if (current == null) return null;
        for (String part : path) current = new File(current, part);
        return current;
    }

    /**
     * Sets the client folder and opens {@link ClientLogFile} ({@code slclient-latest.log}) in it;
     * called once by the mod-loader client.
     */
    public static void setFolder(File clientFolder) {
        folder = clientFolder;
        ClientLogFile.open(clientFolder);
    }

    /** Routes client log output to the game's logger; {@code slclient-latest.log} gets it as well. */
    public static void setLog(BiConsumer<Level, String> sink) {
        if (sink != null) log = sink;
    }

    /**
     * Logs to {@code slclient-latest.log} (when open) and to the game's logger. A failing sink
     * never breaks the caller.
     */
    public static void log(Level level, String message, Throwable error) {
        ClientLogFile.write(level, message, error);
        String line = error == null ? message : message + ": " + error;
        try {
            log.accept(level, line);
        } catch (Throwable t) {
            System.err.println("[StreamlineClient] " + line);
        }
    }

    public static void logInfo(String message) {
        log(Level.INFO, message, null);
    }

    public static void logWarning(String message) {
        log(Level.WARNING, message, null);
    }

    /** Logs a warning; {@code slclient-latest.log} also gets {@code error}'s stack trace. */
    public static void logWarning(String message, Throwable error) {
        log(Level.WARNING, message, error);
    }

    /** Flushes and closes {@code slclient-latest.log}; the game is exiting. */
    public static void closeLog() {
        ClientLogFile.close();
    }
}
