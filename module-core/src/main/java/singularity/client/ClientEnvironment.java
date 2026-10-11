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
 *     <li>{@code hud-layout.properties} where the player placed each HUD element</li>
 *     <li>{@code modules.properties} which modules the player turned off</li>
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

    /** Sets the client folder; called once by the mod-loader client. */
    public static void setFolder(File clientFolder) {
        folder = clientFolder;
    }

    /** Routes client log output to the game's logger. */
    public static void setLog(BiConsumer<Level, String> sink) {
        if (sink != null) log = sink;
    }

    public static void logInfo(String message) {
        log.accept(Level.INFO, message);
    }

    public static void logWarning(String message) {
        log.accept(Level.WARNING, message);
    }
}
