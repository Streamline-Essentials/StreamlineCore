package singularity.updates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Puts a staged StreamlineCore jar in place of the running one as the JVM exits.
 *
 * <p>The old jar is deleted first and the new one moved in only once that worked, so a swap
 * that fails leaves the old version alone instead of two cores in one folder. Where the old
 * jar cannot be deleted from inside the JVM (Windows keeps loaded jars locked):</p>
 * <ul>
 *     <li>with a platform update folder (Bukkit's {@code plugins/update}), the new jar goes
 *     there under the old jar's name, and the server swaps it in on its next start;</li>
 *     <li>otherwise a small helper process ({@link #main}) waits for this JVM to let go of the
 *     old jar and then does the same swap.</li>
 * </ul>
 *
 * <p>The helper runs from a copy of the running jar with nothing but the JDK, so it uses only
 * {@code java.*} classes.</p>
 */
public final class CoreJarSwap {
    private static final String PREFIX = "[StreamlineCore] ";

    /** How long the helper keeps trying to delete the old jar. */
    private static final long HELPER_TIMEOUT_MILLIS = 120_000L;

    private static Thread hook;
    private static volatile Path running;
    private static volatile Path staged;
    private static volatile Path replacement;
    private static volatile Path updateFolder;

    private CoreJarSwap() {
    }

    /**
     * Installs {@code staged} in place of {@code running} when the JVM exits; a later call
     * replaces the earlier one.
     *
     * @param replacement  where the new jar goes (next to {@code running}, usually under its new version's name)
     * @param updateFolder the platform's update folder, or {@code null}
     */
    static synchronized void schedule(Path running, Path staged, Path replacement, Path updateFolder) {
        CoreJarSwap.running = running;
        CoreJarSwap.staged = staged;
        CoreJarSwap.replacement = replacement;
        CoreJarSwap.updateFolder = updateFolder;
        if (hook == null) {
            hook = new Thread(CoreJarSwap::apply, "Streamline-CoreJarSwap");
            Runtime.getRuntime().addShutdownHook(hook);
        }
    }

    private static void apply() {
        Path running = CoreJarSwap.running, staged = CoreJarSwap.staged, replacement = CoreJarSwap.replacement;
        if (running == null || staged == null || ! Files.isRegularFile(staged)) return;

        if (swap(running, staged, replacement)) {
            log("Installed " + replacement.getFileName() + "; it runs from the next start.");
            return;
        }

        Path folder = updateFolder;
        if (folder != null) {
            try {
                Files.createDirectories(folder);
                Files.copy(staged, folder.resolve(running.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(staged);
                log("Placed the update in " + folder + "; the server installs it on its next start.");
                return;
            } catch (IOException e) {
                log("Could not place the update in " + folder + ": " + e.getMessage());
            }
        }

        try {
            startHelper(running, staged, replacement);
            log("The running jar is locked; " + replacement.getFileName() + " is installed once the server has exited.");
        } catch (IOException e) {
            log("Could not install the update (" + e.getMessage() + "). Replace " + running + " with " + staged + " by hand.");
        }
    }

    /**
     * Deletes {@code running} and moves {@code staged} to {@code replacement}.
     *
     * @return whether the new jar is in place; {@code false} with the old jar untouched when it
     *         could not be deleted
     */
    static boolean swap(Path running, Path staged, Path replacement) {
        try {
            Files.deleteIfExists(running);
        } catch (IOException e) {
            return false;
        }
        try {
            Files.move(staged, replacement, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // The old jar is gone, so this has to succeed one way or another.
            try {
                Files.copy(staged, replacement, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException again) {
                log("The old jar was removed but " + replacement + " could not be written (" + again.getMessage()
                        + "). Copy " + staged + " there by hand.");
                return true;
            }
        }
        return true;
    }

    /**
     * Starts {@link #main} in a separate JVM. It runs from a copy of the running jar, which is
     * sure to hold this class (the staged build may come from any version), and which it can
     * keep locked while it replaces the original.
     */
    private static void startHelper(Path running, Path staged, Path replacement) throws IOException {
        Path helperJar = staged.resolveSibling("swap-helper.jar.tmp");
        Files.copy(running, helperJar, StandardCopyOption.REPLACE_EXISTING);

        String java = Paths.get(System.getProperty("java.home"), "bin",
                File.separatorChar == '\\' ? "java.exe" : "java").toString();
        List<String> command = new ArrayList<>();
        command.add(java);
        command.add("-Xmx32m");
        command.add("-cp");
        command.add(helperJar.toString());
        command.add(CoreJarSwap.class.getName());
        command.add(running.toString());
        command.add(staged.toString());
        command.add(replacement.toString());

        File log = staged.resolveSibling("swap.log").toFile();
        new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(log))
                .redirectInput(ProcessBuilder.Redirect.from(nullFile()))
                .start();
    }

    private static File nullFile() {
        return new File(File.separatorChar == '\\' ? "NUL" : "/dev/null");
    }

    /**
     * Helper entry point: {@code <running jar> <staged jar> <replacement>}. Retries the swap
     * until the old jar can be deleted, which is once the server's JVM has exited.
     */
    public static void main(String[] args) throws InterruptedException {
        if (args.length != 3) {
            log("usage: CoreJarSwap <running jar> <staged jar> <replacement>");
            System.exit(2);
        }
        Path running = Paths.get(args[0]), staged = Paths.get(args[1]), replacement = Paths.get(args[2]);
        long deadline = System.currentTimeMillis() + HELPER_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (! Files.isRegularFile(staged)) {
                log("Nothing to install: " + staged + " is gone.");
                System.exit(0);
            }
            if (swap(running, staged, replacement)) {
                log("Installed " + replacement.getFileName() + ".");
                System.exit(0);
            }
            Thread.sleep(500L);
        }
        log("Gave up: " + running + " stayed locked. Replace it with " + staged + " by hand.");
        System.exit(1);
    }

    private static void log(String message) {
        System.out.println(PREFIX + message);
    }
}
