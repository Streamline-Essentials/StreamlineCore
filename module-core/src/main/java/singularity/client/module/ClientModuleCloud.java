package singularity.client.module;

import lombok.Getter;
import singularity.registry.ModuleRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The game client's view of the module registry (eCloud): only modules whose platforms
 * include {@code modded-client} are listed, downloaded or updated, and a download is checked
 * twice: the registry must list the version for clients, and the jar itself must say so
 * before {@link ClientModuleManager} loads it.
 *
 * <p>Downloads load straight away, replacing an installed version of the same module without
 * a restart.</p>
 */
public final class ClientModuleCloud {

    private static final ModuleRegistry REGISTRY = new ModuleRegistry();

    /** Minimum gap between name fetches triggered by tab completion. */
    private static final long COMPLETION_REFRESH_GAP_MILLIS = 30_000L;

    /** Network requests run here, never on the render thread or the module worker. */
    private static final ExecutorService NETWORK = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "Streamline-ClientCloud");
        thread.setDaemon(true);
        return thread;
    });

    private static final ConcurrentSkipListSet<String> cachedNames = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
    private static final AtomicBoolean refreshing = new AtomicBoolean();
    private static volatile long refreshedAt;

    private ClientModuleCloud() {}

    /** Outcome of {@link #download} or {@link #update}. */
    @Getter
    public static final class Result {
        public enum Status {
            /** The module was not installed and now is. */
            INSTALLED,
            /** Another version replaced the installed one. */
            UPDATED,
            /** The installed version is the registry's latest, or newer. */
            UP_TO_DATE,
            /** {@link #update} was asked for a module that is not installed. */
            NOT_INSTALLED,
        }

        private final Status status;
        private final String moduleId;
        /** The version installed before, or {@code null}. */
        private final String oldVersion;
        /** The version installed now, or the registry's latest when nothing changed. */
        private final String version;
        /** Whether the module is enabled now. */
        private final boolean enabled;

        Result(Status status, String moduleId, String oldVersion, String version, boolean enabled) {
            this.status = status;
            this.moduleId = moduleId;
            this.oldVersion = oldVersion;
            this.version = version;
            this.enabled = enabled;
        }
    }

    /** Every client module the registry lists. */
    public static CompletableFuture<List<ModuleRegistry.ModuleInfo>> available() {
        return CompletableFuture.supplyAsync(() -> {
            List<ModuleRegistry.ModuleInfo> modules = REGISTRY.listNow(ClientModuleManager.CLIENT_TAG);
            List<String> names = new ArrayList<>();
            for (ModuleRegistry.ModuleInfo info : modules) names.add(info.getName());
            cachedNames.retainAll(names);
            cachedNames.addAll(names);
            refreshedAt = System.currentTimeMillis();
            return modules;
        }, NETWORK);
    }

    /** A client module's registry entry; fails if the registry does not list it for clients. */
    public static CompletableFuture<ModuleRegistry.ModuleInfo> info(String name) {
        return CompletableFuture.supplyAsync(() -> requireClient(REGISTRY.infoNow(name), null), NETWORK);
    }

    /**
     * Client module names for tab completion. Never blocks: returns what is cached and
     * refreshes it in the background at most every {@link #COMPLETION_REFRESH_GAP_MILLIS}.
     */
    public static List<String> cachedNames() {
        if (System.currentTimeMillis() - refreshedAt >= COMPLETION_REFRESH_GAP_MILLIS && refreshing.compareAndSet(false, true)) {
            refreshedAt = System.currentTimeMillis();
            available().whenComplete((modules, error) -> refreshing.set(false));
        }
        return new ArrayList<>(cachedNames);
    }

    /**
     * Downloads a client module and loads it, replacing an installed version of it.
     *
     * @param name    registry name or Plugin-Id
     * @param version a specific version, or {@code null} for the latest
     * @return the result; fails with a {@link ModuleRegistry.RegistryException} the player can read
     */
    public static CompletableFuture<Result> download(String name, String version) {
        return CompletableFuture.supplyAsync(() -> {
            ModuleRegistry.ModuleInfo info = requireClient(REGISTRY.infoNow(name), version);
            String id = info.getId().isEmpty() ? name : info.getId();
            String target = version == null || version.isBlank() ? info.getVersion() : version.trim();
            ClientModuleManager.Installed installed = ClientModuleManager.module(id);
            if (installed != null && installed.getVersion().equalsIgnoreCase(target)) {
                throw new ModuleRegistry.RegistryException("'" + installed.getId() + "' " + installed.getVersion() + " is already installed.");
            }
            return fetchAndInstall(id, version, installed == null ? null : installed.getVersion());
        }, NETWORK).thenCompose(future -> future);
    }

    /** Replaces an installed client module with the registry's latest version, if that is newer. */
    public static CompletableFuture<Result> update(String name) {
        return CompletableFuture.supplyAsync(() -> {
            ModuleRegistry.ModuleInfo info = requireClient(REGISTRY.infoNow(name), null);
            String id = info.getId().isEmpty() ? name : info.getId();
            ClientModuleManager.Installed installed = ClientModuleManager.module(id);
            if (installed == null) installed = ClientModuleManager.module(name);
            if (installed == null) {
                // A jar of it that did not load counts as installed: updating is how it gets fixed.
                String unloaded = ClientModuleManager.unloadedVersion(id);
                if (unloaded == null) {
                    return CompletableFuture.completedFuture(new Result(Result.Status.NOT_INSTALLED, id, null, info.getVersion(), false));
                }
                if (unloaded.equalsIgnoreCase(info.getVersion())) {
                    throw new ModuleRegistry.RegistryException("'" + id + "' " + unloaded
                            + " is eCloud's latest version, and it did not load here. See /slclient modules list.");
                }
                return fetchAndInstall(id, info.getVersion(), unloaded);
            }
            if (ModuleRegistry.compareVersions(installed.getVersion(), info.getVersion()) >= 0) {
                return CompletableFuture.completedFuture(new Result(Result.Status.UP_TO_DATE, installed.getId(),
                        installed.getVersion(), info.getVersion(), installed.isEnabled()));
            }
            return fetchAndInstall(id, info.getVersion(), installed.getVersion());
        }, NETWORK).thenCompose(future -> future);
    }

    private static CompletableFuture<Result> fetchAndInstall(String id, String version, String oldVersion) {
        ModuleRegistry.Fetched fetched = REGISTRY.fetch(id, version, ClientModuleManager.folder().toPath());
        return ClientModuleManager.install(fetched)
                .handle((installed, error) -> {
                    if (error != null) {
                        Throwable cause = error.getCause() != null ? error.getCause() : error;
                        throw new ModuleRegistry.RegistryException(String.valueOf(cause.getMessage()), cause);
                    }
                    return new Result(oldVersion == null ? Result.Status.INSTALLED : Result.Status.UPDATED,
                            installed.getId(), oldVersion, installed.getVersion(), installed.isEnabled());
                });
    }

    /** Refuses modules (or the requested version) the registry does not list for clients. */
    private static ModuleRegistry.ModuleInfo requireClient(ModuleRegistry.ModuleInfo info, String version) {
        if (! ClientModuleManager.isStarted()) {
            throw new ModuleRegistry.RegistryException("Client modules are not running in this game.");
        }
        List<String> platforms = info.getPlatforms();
        if (version != null && ! version.isBlank()) {
            ModuleRegistry.VersionInfo listed = info.version(version.trim());
            if (listed == null) {
                throw new ModuleRegistry.RegistryException("'" + info.getName() + "' has no version " + version.trim() + ".");
            }
            platforms = listed.getPlatforms();
        }
        if (! platforms.contains(ClientModuleManager.CLIENT_TAG)) {
            throw new ModuleRegistry.RegistryException("'" + info.getName() + "' is not a client module"
                    + (platforms.isEmpty() ? "." : " (it runs on " + String.join(", ", platforms) + ")."));
        }
        return info;
    }
}
