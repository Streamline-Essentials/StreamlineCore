package singularity.modules;

import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.Singularity;
import singularity.interfaces.ISingularityExtension;
import singularity.registry.ModuleRegistry;
import singularity.registry.ModuleRegistry.ModuleInfo;
import singularity.registry.ModuleRegistry.RegistryException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Client for the Streamline module registry ("eCloud", {@code https://modules.drak.gg}).
 *
 * <p>Downloads land in the module folder under the file name the registry
 * suggests ({@code <Name>-<version>.jar}) and are verified against the
 * registry's SHA-256 before they are moved into place, so a truncated or
 * corrupted transfer never becomes a loadable jar. The registry requests themselves go
 * through {@link ModuleRegistry}, which the game client's module browser shares.</p>
 */
public final class ModuleCloud {
    /** Registry root used when nothing else is configured. */
    public static final String DEFAULT_BASE_URL = ModuleRegistry.DEFAULT_BASE_URL;

    /** Default period of the module-name refresh timer. */
    public static final Duration DEFAULT_NAME_REFRESH_INTERVAL = Duration.ofMinutes(5);

    /**
     * Minimum gap between fetches triggered by tab completion. Completion fires
     * on every keystroke; this keeps typing a module name to at most one request.
     */
    private static final long COMPLETION_REFRESH_GAP_MILLIS = Duration.ofSeconds(10).toMillis();

    @Getter @Setter
    private static String baseUrl = DEFAULT_BASE_URL;

    private static final ModuleRegistry REGISTRY = new ModuleRegistry(() -> baseUrl);

    /** How soon a failed module-name fetch is retried, independent of the timer period. */
    private static final long NAME_RETRY_MILLIS = Duration.ofSeconds(30).toMillis();

    /**
     * Runs module-name fetches and the refresh timer. A daemon thread of its own
     * keeps requests off the server thread and the common pool (which mods may
     * saturate), and never holds up shutdown. Being a single thread, two fetches
     * never run at once.
     */
    private static final ScheduledExecutorService NAME_FETCHER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Streamline-eCloud");
        thread.setDaemon(true);
        return thread;
    });

    private static final ConcurrentSkipListSet<String> cachedNames = new ConcurrentSkipListSet<>();
    /** When the last fetch started, for throttling completion-triggered fetches. */
    private static volatile long namesFetchStartedAt = 0L;
    /** Set while a fetch is queued or running, so triggers never pile up behind it. */
    private static final AtomicBoolean namesRefreshing = new AtomicBoolean(false);
    private static ScheduledFuture<?> refreshTimer;
    private static ScheduledFuture<?> retryTask;

    private ModuleCloud() {}

    /** Outcome of a successful {@link #download}. */
    @Getter
    public static final class Download {
        private final String moduleId;
        private final String version;
        private final Path file;
        /** Whether the module was loaded and started after saving. */
        private final boolean loaded;
        /** Why the saved jar was not loaded, when {@link #isLoaded()} is false. */
        private final String loadError;

        Download(String moduleId, String version, Path file, boolean loaded, String loadError) {
            this.moduleId = moduleId;
            this.version = version;
            this.file = file;
            this.loaded = loaded;
            this.loadError = loadError;
        }
    }

    /**
     * Looks a module up in the registry by name or Plugin-Id, ignoring case.
     *
     * @param name the module's registry name or Plugin-Id
     * @return the module's registry entry; fails with a {@link RegistryException} when the
     *         registry does not know it or cannot be reached
     */
    public static CompletableFuture<ModuleInfo> info(String name) {
        return REGISTRY.info(name);
    }

    /**
     * Downloads a module from the registry into the module folder and loads it.
     * The transfer and checksum run off-thread; installing and loading the jar
     * run through {@link ISingularityExtension#runOnMainThread(Runnable)}.
     *
     * @param name    the module name or Plugin-Id, matched case-insensitively by the registry
     * @param version a specific version, or {@code null} for the latest
     * @return a future completed with the result, or exceptionally with a {@link RegistryException}
     */
    public static CompletableFuture<Download> download(String name, String version) {
        return CompletableFuture.supplyAsync(() -> fetch(name, version))
                .thenCompose(fetched -> onMainThread(() -> install(fetched)));
    }

    /** Outcome of {@link #update}. */
    @Getter
    public static final class Update {
        public enum Status {
            /** The new version replaced the old jar. */
            UPDATED,
            /** The installed version is the registry's latest, or newer. */
            UP_TO_DATE,
            /** No jar in the module folder is that module. */
            NOT_INSTALLED,
        }

        private final Status status;
        private final String moduleId;
        /** The version that was installed, or {@code null} when {@link Status#NOT_INSTALLED}. */
        private final String oldVersion;
        /** The registry's latest version. */
        private final String version;
        /** The new jar, when {@link Status#UPDATED}. */
        private final Path file;
        /** Whether the new version was loaded and started. */
        private final boolean loaded;
        /** Why the new jar was not loaded, when it was saved but {@link #isLoaded()} is false. */
        private final String loadError;
        /** The old jar's name when it could not be deleted (it is deleted when the server stops), else {@code null}. */
        private final String leftoverJar;

        Update(Status status, String moduleId, String oldVersion, String version, Path file,
               boolean loaded, String loadError, String leftoverJar) {
            this.status = status;
            this.moduleId = moduleId;
            this.oldVersion = oldVersion;
            this.version = version;
            this.file = file;
            this.loaded = loaded;
            this.loadError = loadError;
            this.leftoverJar = leftoverJar;
        }
    }

    /**
     * Replaces an installed module with the registry's latest version, if that is newer or is
     * the installed version with a different jar ({@link ModuleRegistry#updateAvailable}). The
     * old module is unloaded and its jar deleted, then the new jar is saved and loaded; modules
     * that depend on it are unloaded with it and loaded again afterwards. Data folders and
     * configs are kept.
     *
     * @param name the module's registry name or Plugin-Id
     * @return a future completed with the result, or exceptionally with a {@link RegistryException}
     */
    public static CompletableFuture<Update> update(String name) {
        return info(name).thenCompose(info -> {
            String id = info.getId().isEmpty() ? name : info.getId();
            Path oldJar = ModuleManager.findModuleJar(id);
            if (oldJar == null && ! id.equalsIgnoreCase(name)) oldJar = ModuleManager.findModuleJar(name);
            if (oldJar == null) {
                return CompletableFuture.completedFuture(new Update(Update.Status.NOT_INSTALLED, id, null, info.getVersion(),
                        null, false, null, null));
            }
            String oldVersion = installedVersion(oldJar);
            if (oldVersion != null && ! ModuleRegistry.updateAvailable(oldVersion, oldJar, info)) {
                return CompletableFuture.completedFuture(new Update(Update.Status.UP_TO_DATE, id, oldVersion, info.getVersion(),
                        oldJar, true, null, null));
            }
            Path replaced = oldJar;
            return CompletableFuture.supplyAsync(() -> fetch(id, info.getVersion()))
                    .thenCompose(fetched -> onMainThread(() -> replace(fetched, replaced, oldVersion)));
        });
    }

    /** The version of the module in {@code jar}: what PF4J loaded, else what its manifest declares. */
    private static String installedVersion(Path jar) {
        PluginWrapper wrapper = ModuleManager.getPluginWrapperByJarName(jar.getFileName().toString());
        if (wrapper != null) return wrapper.getDescriptor().getVersion();
        return ModuleManager.manifestValue(jar.toFile(), "Plugin-Version");
    }

    /** @see ModuleRegistry#compareVersions */
    static int compareVersions(String a, String b) {
        return ModuleRegistry.compareVersions(a, b);
    }

    private static Update replace(Fetched fetched, Path oldJar, String oldVersion) {
        try {
            String oldName = oldJar.getFileName().toString();
            // Checked before anything is unloaded, so a refused update leaves the old module running.
            String clash = jarOnDiskWithId(fetched.moduleId, fetched.fileName);
            if (clash != null && ! clash.equals(oldName)) {
                throw new RegistryException("'" + clash + "' in the module folder already has the Plugin-Id '"
                        + fetched.moduleId + "'. Delete it first.");
            }

            PluginWrapper oldWrapper = ModuleManager.getPluginWrapperByJarName(oldName);
            List<String> dependents = oldWrapper == null ? new java.util.ArrayList<>() : ModuleManager.getDependentJarNames(oldWrapper.getPluginId());
            ModuleManager.unloadModuleJar(oldJar);

            Path target = Singularity.getModuleFolder().toPath().resolve(fetched.fileName);
            String leftover = null;
            if (! oldJar.equals(target)) {
                try {
                    Files.deleteIfExists(oldJar);
                } catch (IOException e) {
                    // Windows refuses to delete a jar while any handle to it is still open.
                    oldJar.toFile().deleteOnExit();
                    leftover = oldName;
                }
            }

            try {
                ModuleRegistry.move(fetched.temp, target);
            } catch (IOException e) {
                if (oldJar.toFile().isFile()) reload(oldName, dependents);
                throw new RegistryException("Could not save " + fetched.fileName + ": " + e.getMessage(), e);
            }

            boolean loaded;
            String loadError = null;
            try {
                ModuleManager.registerExternalModule(fetched.fileName);
                loaded = ModuleManager.getPluginWrapperByJarName(fetched.fileName) != null;
                if (! loaded) loadError = "PF4J did not register the jar";
            } catch (Throwable e) {
                loaded = false;
                loadError = String.valueOf(e.getMessage());
            }

            if (loaded) reload(null, dependents);
            return new Update(Update.Status.UPDATED, fetched.moduleId, oldVersion, fetched.version, target, loaded, loadError, leftover);
        } finally {
            deleteQuietly(fetched.temp);
        }
    }

    /** Loads {@code jar} (when not {@code null}) and then each dependent jar that is not loaded. */
    private static void reload(String jar, List<String> dependents) {
        List<String> jars = new java.util.ArrayList<>();
        if (jar != null) jars.add(jar);
        jars.addAll(dependents);
        for (String name : jars) {
            if (ModuleManager.getPluginWrapperByJarName(name) != null) continue;
            try {
                ModuleManager.registerExternalModule(name);
            } catch (Throwable e) {
                singularity.utils.MessageUtils.logWarning("Could not load '" + name + "' again: " + e.getMessage());
            }
        }
    }

    /** A verified jar sitting in a temporary file in the module folder. */
    private static final class Fetched {
        final Path temp;
        final String moduleId;
        final String version;
        final String fileName;

        Fetched(ModuleRegistry.Fetched fetched) {
            this.temp = fetched.getTemp();
            this.moduleId = fetched.getModuleId();
            this.version = fetched.getVersion();
            this.fileName = fetched.getFileName();
        }
    }

    private static Fetched fetch(String name, String version) {
        return new Fetched(REGISTRY.fetch(name, version, Singularity.getModuleFolder().toPath()));
    }

    private static Download install(Fetched fetched) {
        try {
            PluginWrapper installed = ModuleManager.safePluginManager().getPlugin(fetched.moduleId);
            if (installed != null) {
                String installedVersion = installed.getDescriptor().getVersion();
                String installedJar = installed.getPluginPath().getFileName().toString();
                if (installedVersion.equals(fetched.version)) {
                    throw new RegistryException("'" + fetched.moduleId + "' " + fetched.version + " is already installed (" + installedJar + ").");
                }
                // PF4J identifies modules by Plugin-Id, so a second jar with the same id
                // would clash on the next start; replacing a loaded jar needs a restart.
                throw new RegistryException("'" + fetched.moduleId + "' " + installedVersion + " is already installed (" + installedJar
                        + "). Use /modules ecloud update " + fetched.moduleId + " to switch to the latest version.");
            }

            // Jars PF4J has not loaded (unloaded, or failed to load) still claim their
            // Plugin-Id on the next start. A jar with the target name is simply replaced.
            String clash = jarOnDiskWithId(fetched.moduleId, fetched.fileName);
            if (clash != null) {
                throw new RegistryException("'" + clash + "' in the module folder already has the Plugin-Id '"
                        + fetched.moduleId + "'. Delete it first.");
            }

            Path target = Singularity.getModuleFolder().toPath().resolve(fetched.fileName);
            try {
                ModuleRegistry.move(fetched.temp, target);
            } catch (IOException e) {
                throw new RegistryException("Could not save " + fetched.fileName + ": " + e.getMessage(), e);
            }

            try {
                ModuleManager.registerExternalModule(fetched.fileName);
            } catch (Throwable e) {
                return new Download(fetched.moduleId, fetched.version, target, false, String.valueOf(e.getMessage()));
            }
            boolean loaded = ModuleManager.getPluginWrapperByJarName(fetched.fileName) != null;
            return new Download(fetched.moduleId, fetched.version, target, loaded, loaded ? null : "PF4J did not register the jar");
        } finally {
            deleteQuietly(fetched.temp);
        }
    }

    /** The name of a jar in the module folder, other than {@code except}, whose manifest declares {@code pluginId}. */
    private static String jarOnDiskWithId(String pluginId, String except) {
        for (File file : ModuleManager.getModuleFiles().values()) {
            if (file.getName().equals(except)) continue;
            try (JarFile jar = new JarFile(file)) {
                Manifest manifest = jar.getManifest();
                if (manifest == null) continue;
                String id = manifest.getMainAttributes().getValue("Plugin-Id");
                if (id != null && id.trim().equalsIgnoreCase(pluginId)) return file.getName();
            } catch (IOException ignored) {
                // Unreadable jars cannot be loaded either, so they cannot clash.
            }
        }
        return null;
    }

    private static <T> CompletableFuture<T> onMainThread(Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Singularity.getInstance().getPlatform().runOnMainThread(() -> {
            try {
                future.complete(task.get());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private static void deleteQuietly(Path path) {
        ModuleRegistry.deleteQuietly(path);
    }

    /**
     * Module names known to the registry, for tab completion. Never blocks: it
     * returns the cache as it is and starts a background fetch (at most one per
     * {@link #COMPLETION_REFRESH_GAP_MILLIS}), whose result lands in the cache
     * for the next completion.
     */
    public static ConcurrentSkipListSet<String> getCachedModuleNames() {
        if (System.currentTimeMillis() - namesFetchStartedAt >= COMPLETION_REFRESH_GAP_MILLIS) refreshModuleNames();
        return cachedNames;
    }

    /**
     * Starts (or restarts with a new period) the timer that refreshes the
     * module-name cache. The first fetch runs immediately, in the background.
     *
     * @param interval time between fetches; non-positive disables the timer, leaving only completion-triggered fetches
     */
    public static synchronized void startNameRefreshTimer(Duration interval) {
        if (refreshTimer != null) refreshTimer.cancel(false);
        refreshTimer = null;
        if (interval == null || interval.isZero() || interval.isNegative()) {
            refreshModuleNames();
            return;
        }
        long millis = interval.toMillis();
        refreshTimer = NAME_FETCHER.scheduleWithFixedDelay(ModuleCloud::refreshModuleNames, 0L, millis, TimeUnit.MILLISECONDS);
    }

    /** Fetches the registry's module names in the background, unless a fetch is already queued or running. */
    public static void refreshModuleNames() {
        if (! namesRefreshing.compareAndSet(false, true)) return;
        namesFetchStartedAt = System.currentTimeMillis();
        try {
            NAME_FETCHER.execute(ModuleCloud::fetchModuleNames);
        } catch (RuntimeException e) {
            namesRefreshing.set(false);
        }
    }

    /**
     * A failed fetch is retried after {@link #NAME_RETRY_MILLIS} rather than a
     * full timer period, so a registry that was unreachable at start-up does
     * not leave completion empty for long.
     */
    private static synchronized void scheduleRetry() {
        if (retryTask != null && ! retryTask.isDone()) return;
        retryTask = NAME_FETCHER.schedule(ModuleCloud::refreshModuleNames, NAME_RETRY_MILLIS, TimeUnit.MILLISECONDS);
    }

    private static void fetchModuleNames() {
        boolean ok = false;
        try {
            ConcurrentSkipListSet<String> names = new ConcurrentSkipListSet<>();
            for (ModuleInfo info : REGISTRY.listNow(null)) names.add(info.getName());
            cachedNames.retainAll(names);
            cachedNames.addAll(names);
            ok = true;
        } catch (Exception ignored) {
            // Completion simply keeps the previous list.
        } finally {
            namesRefreshing.set(false);
            if (! ok) scheduleRetry();
        }
    }
}
