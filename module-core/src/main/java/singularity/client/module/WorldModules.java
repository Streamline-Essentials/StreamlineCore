package singularity.client.module;

import singularity.client.ClientEnvironment;
import singularity.registry.ModuleRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * The server modules a game client runs on the worlds it hosts (singleplayer or LAN), as the
 * client sees them: jars in Streamline's server module folder ({@code modules/}, beside
 * {@code client/}). A module is offered to clients for that when its platforms include both
 * {@code modded-server} and {@code modded-client}; a jar with {@code modded-client} alone is
 * a {@link ClientModule} and belongs to {@link ClientModuleManager}.
 *
 * <p>World modules load with Streamline once the client hosts a world. While Streamline runs,
 * installs and removals go through the {@link Host} the platform sets, which loads and unloads
 * them at once; before that, only the jars change and the next world loads them.</p>
 */
public final class WorldModules {

    /** The platform tag of modules that run on a mod-loader server, dedicated or integrated. */
    public static final String SERVER_TAG = "modded-server";

    /** What the platform does with world modules while Streamline runs for a hosted world. */
    public interface Host {
        /** @return whether Streamline is running, so world modules load and unload at once */
        boolean isRunning();

        /** @return whether a module is loaded from this jar */
        boolean isLoaded(Path jar);

        /**
         * Unloads {@code oldJar} (when not {@code null}) and the modules depending on it, moves
         * {@code temp} to {@code target}, then loads it and those dependents again.
         *
         * @return why the module did not load, or {@code null} if it did
         */
        CompletableFuture<String> install(Path oldJar, Path temp, Path target);

        /** Unloads the module behind {@code jar} and deletes the jar. */
        CompletableFuture<Void> remove(Path jar);
    }

    /** A world module jar in the server module folder. */
    public static final class Installed {
        private final String id;
        private final String version;
        private final Path jar;

        Installed(String id, String version, Path jar) {
            this.id = id;
            this.version = version;
            this.jar = jar;
        }

        public String getId() {
            return id;
        }

        public String getVersion() {
            return version;
        }

        public Path getJar() {
            return jar;
        }

        public String getFileName() {
            return jar.getFileName().toString();
        }

        /** @return whether the module is running now, i.e. a world was hosted and it loaded */
        public boolean isLoaded() {
            Host current = host;
            return current != null && current.isRunning() && current.isLoaded(jar);
        }
    }

    private static volatile boolean supported;
    private static volatile Host host;

    private WorldModules() {}

    /**
     * @param value whether this client can host worlds and so run world modules; Minecraft
     *              1.8.9 runs no Streamline server, so it leaves this off
     */
    public static void setSupported(boolean value) {
        supported = value;
    }

    /** @return whether this client runs world modules in the worlds it hosts */
    public static boolean isSupported() {
        return supported && ClientEnvironment.folder() != null;
    }

    public static void setHost(Host value) {
        host = value;
    }

    /** @return whether the platforms make a world module: one for mod servers that clients may run */
    public static boolean isWorldModule(List<String> platforms) {
        return containsIgnoreCase(platforms, ClientModuleManager.CLIENT_TAG) && containsIgnoreCase(platforms, SERVER_TAG);
    }

    /** @return whether the platforms make a client module, run by {@link ClientModuleManager} */
    public static boolean isClientModule(List<String> platforms) {
        return containsIgnoreCase(platforms, ClientModuleManager.CLIENT_TAG) && ! containsIgnoreCase(platforms, SERVER_TAG);
    }

    /** @return whether a module with these platforms can be installed on this client */
    public static boolean installable(List<String> platforms) {
        return isClientModule(platforms) || (isWorldModule(platforms) && isSupported());
    }

    /** Streamline's server module folder, beside {@code client/}, or {@code null} off a client. */
    public static File folder() {
        File client = ClientEnvironment.folder();
        return client == null ? null : new File(client.getParentFile(), "modules");
    }

    /** The world modules in the server module folder, sorted by id. */
    public static List<Installed> installed() {
        List<Installed> result = new ArrayList<>();
        File folder = folder();
        File[] jars = folder == null ? null : folder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null) return result;
        TreeMap<String, Installed> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (File file : jars) {
            Path jar = file.toPath();
            if (! isWorldModule(ModuleRegistry.manifestPlatforms(jar))) continue;
            String id = ModuleRegistry.manifestValue(jar, "Plugin-Id");
            if (id == null || id.isEmpty()) continue;
            String version = ModuleRegistry.manifestValue(jar, "Plugin-Version");
            sorted.put(id, new Installed(id, version == null ? "0" : version, jar));
        }
        result.addAll(sorted.values());
        return result;
    }

    /** The installed world module with this Plugin-Id, ignoring case, or {@code null}. */
    public static Installed module(String id) {
        if (id == null) return null;
        for (Installed installed : installed()) {
            if (installed.id.equalsIgnoreCase(id)) return installed;
        }
        return null;
    }

    /**
     * Moves a verified download into the server module folder, replacing the installed jar of
     * that module. Loads it at once while Streamline runs, otherwise with the next world.
     *
     * @return the module as installed
     */
    public static CompletableFuture<Installed> install(ModuleRegistry.Fetched fetched) {
        Path temp = fetched.getTemp();
        try {
            if (! isSupported()) throw new IllegalStateException("This game does not run modules in the worlds it hosts.");
            if (! isWorldModule(ModuleRegistry.manifestPlatforms(temp))) {
                throw new IllegalStateException("'" + fetched.getModuleId() + "' does not run in worlds a client hosts.");
            }
            String id = ModuleRegistry.manifestValue(temp, "Plugin-Id");
            if (id == null) id = fetched.getModuleId();
            Installed old = module(id);
            Path target = folder().toPath().resolve(fetched.getFileName());
            String finalId = id;

            Host current = host;
            if (current != null && current.isRunning()) {
                return current.install(old == null ? null : old.jar, temp, target).handle((refusal, error) -> {
                    fetched.discard();
                    if (error != null) throw new IllegalStateException(message(error), error);
                    if (refusal != null) throw new IllegalStateException(refusal);
                    return found(finalId, target);
                });
            }

            if (old != null && ! old.jar.equals(target)) Files.deleteIfExists(old.jar);
            ModuleRegistry.move(temp, target);
            return CompletableFuture.completedFuture(found(finalId, target));
        } catch (IOException | RuntimeException e) {
            fetched.discard();
            CompletableFuture<Installed> failed = new CompletableFuture<>();
            failed.completeExceptionally(e instanceof IOException
                    ? new IllegalStateException("Could not save " + fetched.getFileName() + ": " + e.getMessage(), e) : e);
            return failed;
        }
    }

    /**
     * Unloads a world module, if it runs, and deletes its jar. Its data and configs stay.
     *
     * @return the deleted jar's file name
     */
    public static CompletableFuture<String> remove(String id) {
        Installed installed = module(id);
        if (installed == null) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("No world module '" + id + "' is installed."));
            return failed;
        }
        Host current = host;
        if (current != null && current.isRunning()) {
            return current.remove(installed.jar).thenApply(ignored -> installed.getFileName());
        }
        try {
            Files.deleteIfExists(installed.jar);
        } catch (IOException e) {
            installed.jar.toFile().deleteOnExit();
        }
        return CompletableFuture.completedFuture(installed.getFileName());
    }

    /**
     * Moves a world module jar that was put in the client module folder to the server module
     * folder, where it runs.
     *
     * @return whether it was moved
     */
    static boolean adopt(Path jar) {
        File folder = folder();
        if (folder == null || ! isSupported()) return false;
        Path target = folder.toPath().resolve(jar.getFileName().toString());
        try {
            Files.createDirectories(folder.toPath());
            if (Files.exists(target)) return false;
            Files.move(jar, target);
            ClientEnvironment.logInfo("Moved world module " + jar.getFileName() + " to " + folder
                    + "; it runs in the worlds this client hosts.");
            return true;
        } catch (IOException e) {
            ClientEnvironment.logWarning("Could not move world module " + jar.getFileName() + " to " + folder + ": " + e.getMessage());
            return false;
        }
    }

    private static Installed found(String id, Path target) {
        Installed installed = module(id);
        if (installed != null) return installed;
        String version = ModuleRegistry.manifestValue(target, "Plugin-Version");
        return new Installed(id, version == null ? "0" : version, target);
    }

    private static String message(Throwable error) {
        Throwable cause = error.getCause() != null ? error.getCause() : error;
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    private static boolean containsIgnoreCase(List<String> values, String wanted) {
        if (values == null) return false;
        for (String value : values) {
            if (value != null && value.trim().toLowerCase(Locale.ROOT).equals(wanted)) return true;
        }
        return false;
    }
}
