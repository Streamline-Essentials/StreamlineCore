package singularity.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

/**
 * StreamlineClient's own log, {@code slclient-latest.log} in the client folder.
 *
 * <p>Opening it gzips the previous session's {@code slclient-latest.log} to
 * {@code slclient-<yyyy-MM-dd_HH-mm-ss>.log.gz}, stamped with the time that session last wrote
 * to it, and keeps only the newest {@link #KEPT_ARCHIVES} archives. Every line is flushed as it
 * is written, so the file is complete up to a crash. Plain {@code java.io} and
 * {@code java.util.zip} only: this runs on the Java 8 1.8.9 client after downgrading.</p>
 */
public final class ClientLogFile {

    public static final String LATEST = "slclient-latest.log";
    private static final String ARCHIVE_PREFIX = "slclient-";
    private static final String ARCHIVE_SUFFIX = ".log.gz";

    /** How many rotated {@code .log.gz} files are kept; older ones are deleted at start. */
    public static final int KEPT_ARCHIVES = 10;

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String LINE = System.getProperty("line.separator", "\n");

    private static final Object LOCK = new Object();
    private static File folder;
    private static Writer writer;
    private static Thread shutdownHook;

    /** {@code SimpleDateFormat} is not thread-safe; it is only used while holding {@link #LOCK}. */
    private static final SimpleDateFormat LINE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private ClientLogFile() {}

    /**
     * Rotates the previous log and opens a fresh {@code slclient-latest.log} in {@code clientFolder}.
     * Does nothing if that folder's log is already open; a different folder closes the open one first.
     * Failures are reported to {@code System.err} and leave file logging off.
     */
    public static void open(File clientFolder) {
        if (clientFolder == null) return;
        synchronized (LOCK) {
            File target = clientFolder.getAbsoluteFile();
            if (writer != null && target.equals(folder)) return;
            closeLocked();
            try {
                if (! target.isDirectory() && ! target.mkdirs() && ! target.isDirectory()) {
                    throw new IOException("could not create " + target);
                }
                File latest = new File(target, LATEST);
                rotate(target, latest);
                prune(target);
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(latest, false), UTF_8));
                folder = target;
                if (shutdownHook == null) {
                    shutdownHook = new Thread(ClientLogFile::close, "StreamlineClient log closer");
                    try {
                        Runtime.getRuntime().addShutdownHook(shutdownHook);
                    } catch (IllegalStateException | SecurityException ignored) {
                        // The JVM is already exiting or forbids hooks; close() still runs from the game's own shutdown.
                    }
                }
            } catch (IOException | RuntimeException e) {
                writer = null;
                folder = null;
                System.err.println("[StreamlineClient] Could not open " + LATEST + " in " + target + ": " + e);
            }
        }
    }

    /** Appends one timestamped line, plus {@code error}'s stack trace if given, and flushes it. */
    public static void write(Level level, String message, Throwable error) {
        synchronized (LOCK) {
            if (writer == null) return;
            try {
                StringBuilder line = new StringBuilder(64 + (message == null ? 4 : message.length()));
                line.append('[').append(LINE_TIME.format(new Date())).append("] [")
                        .append(Thread.currentThread().getName()).append('/')
                        .append(levelName(level)).append("] ")
                        .append(message).append(LINE);
                if (error != null) line.append(stackTrace(error));
                writer.write(line.toString());
                writer.flush();
            } catch (IOException e) {
                System.err.println("[StreamlineClient] Could not write " + LATEST + "; file logging is off: " + e);
                closeLocked();
            }
        }
    }

    /** Flushes and closes the log; later lines reach the game log only. Safe to call more than once. */
    public static void close() {
        synchronized (LOCK) {
            closeLocked();
        }
    }

    /** @return whether lines are currently written to {@code slclient-latest.log} */
    public static boolean isOpen() {
        synchronized (LOCK) {
            return writer != null;
        }
    }

    private static void closeLocked() {
        Writer open = writer;
        writer = null;
        folder = null;
        if (open == null) return;
        try {
            open.close();
        } catch (IOException ignored) {
            // Nothing more can be written either way.
        }
    }

    /** Gzips a non-empty {@code latest} to a uniquely named archive, then removes it. */
    private static void rotate(File dir, File latest) throws IOException {
        if (! latest.isFile()) return;
        if (latest.length() == 0) {
            latest.delete();
            return;
        }
        long modified = latest.lastModified();
        String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                .format(new Date(modified > 0 ? modified : System.currentTimeMillis()));
        File archive = new File(dir, ARCHIVE_PREFIX + stamp + ARCHIVE_SUFFIX);
        for (int n = 1; archive.exists(); n++) {
            archive = new File(dir, ARCHIVE_PREFIX + stamp + "-" + n + ARCHIVE_SUFFIX);
        }
        try (InputStream in = new FileInputStream(latest);
             OutputStream out = new GZIPOutputStream(new FileOutputStream(archive))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        } catch (IOException e) {
            archive.delete();
            throw e;
        }
        if (modified > 0) archive.setLastModified(modified);
        // A latest log that cannot be deleted (held open elsewhere) is truncated when reopened.
        latest.delete();
    }

    /** Deletes all but the newest {@link #KEPT_ARCHIVES} rotated logs. */
    private static void prune(File dir) {
        File[] found = dir.listFiles((d, name) -> name.startsWith(ARCHIVE_PREFIX) && name.endsWith(ARCHIVE_SUFFIX));
        if (found == null || found.length <= KEPT_ARCHIVES) return;
        List<File> archives = new ArrayList<>(Arrays.asList(found));
        // Newest first: by the session time each archive is stamped with, then by name.
        Collections.sort(archives, Comparator.comparingLong(File::lastModified).thenComparing(File::getName).reversed());
        for (int i = KEPT_ARCHIVES; i < archives.size(); i++) archives.get(i).delete();
    }

    private static String levelName(Level level) {
        if (level == null) return "INFO";
        int value = level.intValue();
        if (value >= Level.SEVERE.intValue()) return "ERROR";
        if (value >= Level.WARNING.intValue()) return "WARN";
        if (value >= Level.INFO.intValue()) return "INFO";
        return "DEBUG";
    }

    private static String stackTrace(Throwable error) {
        StringWriter out = new StringWriter();
        PrintWriter printer = new PrintWriter(out);
        error.printStackTrace(printer);
        printer.flush();
        return out.toString();
    }
}
