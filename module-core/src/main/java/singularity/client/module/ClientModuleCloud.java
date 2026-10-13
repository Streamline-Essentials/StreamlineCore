package singularity.client.module;

import lombok.Getter;
import singularity.registry.ModuleRegistry;

import java.nio.file.Path;
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
 * before it is installed.
 *
 * <p>Two kinds are offered. Client modules ({@code modded-client} only) go to
 * {@link ClientModuleManager} and load straight away, replacing an installed version without
 * a restart. World modules ({@code modded-server} and {@code modded-client}) are server modules
 * that also run in the worlds the client hosts; they go to {@link WorldModules}, and only to a
 * client that can host worlds.</p>
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
        /** Whether the module is enabled now; for a world module, whether it is running in a hosted world. */
        private final boolean enabled;
        /** Whether it is a world module, which runs in the worlds this client hosts. */
        private final boolean world;

        Result(Status status, String moduleId, String oldVersion, String version, boolean enabled, boolean world) {
            this.status = status;
            this.moduleId = moduleId;
            this.oldVersion = oldVersion;
            this.version = version;
            this.enabled = enabled;
            this.world = world;
        }
    }

    /**
     * Every module the registry lists that this client can install: client modules, and world
     * modules where this client hosts worlds.
     */
    public static CompletableFuture<List<ModuleRegistry.ModuleInfo>> available() {
        return CompletableFuture.supplyAsync(() -> {
            List<ModuleRegistry.ModuleInfo> modules = new ArrayList<>();
            for (ModuleRegistry.ModuleInfo info : REGISTRY.listNow(ClientModuleManager.CLIENT_TAG)) {
                if (WorldModules.installable(info.getPlatforms())) modules.add(info);
            }
            List<String> names = new ArrayList<>();
            for (ModuleRegistry.ModuleInfo info : modules) names.add(info.getName());
            cachedNames.retainAll(names);
            cachedNames.addAll(names);
            refreshedAt = System.currentTimeMillis();
            return modules;
        }, NETWORK);
    }

    /** A module's registry entry; fails if the registry does not list it for this client. */
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

    /** @return whether the registry entry is a world module, by its latest version's platforms */
    public static boolean isWorldModule(ModuleRegistry.ModuleInfo info) {
        return WorldModules.isWorldModule(info.getPlatforms());
    }

    /** The installed version of a client or world module, by Plugin-Id, or {@code null}. */
    public static String installedVersion(String id) {
        ClientModuleManager.Installed client = ClientModuleManager.module(id);
        if (client != null) return client.getVersion();
        WorldModules.Installed world = WorldModules.module(id);
        return world == null ? null : world.getVersion();
    }

    /** The jar of the installed client or world module, by Plugin-Id, or {@code null}. */
    public static Path installedJar(String id) {
        ClientModuleManager.Installed client = ClientModuleManager.module(id);
        if (client != null) return client.getJar();
        WorldModules.Installed world = WorldModules.module(id);
        return world == null ? null : world.getJar();
    }

    /**
     * Whether eCloud has an update for the installed module: a newer version, or the same
     * version with a jar whose SHA-256 differs from the installed one. {@code false} when the
     * module is not installed. Jar hashes are cached, so screens may ask every frame.
     */
    public static boolean updateAvailable(ModuleRegistry.ModuleInfo info) {
        String key = info.getId().isEmpty() ? info.getName() : info.getId();
        return ModuleRegistry.updateAvailable(installedVersion(key), installedJar(key), info);
    }

    /**
     * Downloads a module and installs it, replacing an installed version of it. A client
     * module loads at once; a world module loads at once while Streamline runs for a hosted
     * world, otherwise with the next one.
     *
     * @param name    registry name or Plugin-Id
     * @param version a specific version, or {@code null} for the latest
     * @return the result; fails with a {@link ModuleRegistry.RegistryException} the player can read
     */
    public static CompletableFuture<Result> download(String name, String version) {
        return logged(CompletableFuture.supplyAsync(() -> {
            ModuleRegistry.ModuleInfo info = requireClient(REGISTRY.infoNow(name), version);
            String id = info.getId().isEmpty() ? name : info.getId();
            String target = version == null || version.isBlank() ? info.getVersion() : version.trim();
            if (worldVersion(info, version)) {
                WorldModules.Installed installed = WorldModules.module(id);
                if (installed != null && installed.getVersion().equalsIgnoreCase(target)
                        && ModuleRegistry.matchesRegistry(info, target, installed.getJar())) {
                    throw new ModuleRegistry.RegistryException("'" + installed.getId() + "' " + installed.getVersion() + " is already installed.");
                }
                return fetchAndInstallWorld(id, version, installed == null ? null : installed.getVersion());
            }
            ClientModuleManager.Installed installed = ClientModuleManager.module(id);
            if (installed != null && installed.getVersion().equalsIgnoreCase(target)
                    && ModuleRegistry.matchesRegistry(info, target, installed.getJar())) {
                throw new ModuleRegistry.RegistryException("'" + installed.getId() + "' " + installed.getVersion() + " is already installed.");
            }
            return fetchAndInstall(id, version, installed == null ? null : installed.getVersion());
        }, NETWORK).thenCompose(future -> future), "download", name);
    }

    /**
     * Replaces an installed module with the registry's latest version, if that is newer or is
     * the installed version with a different jar ({@link ModuleRegistry#updateAvailable}).
     */
    public static CompletableFuture<Result> update(String name) {
        return logged(CompletableFuture.supplyAsync(() -> {
            ModuleRegistry.ModuleInfo info = requireClient(REGISTRY.infoNow(name), null);
            String id = info.getId().isEmpty() ? name : info.getId();
            if (isWorldModule(info)) {
                WorldModules.Installed installed = WorldModules.module(id);
                if (installed == null) installed = WorldModules.module(name);
                if (installed == null) {
                    return CompletableFuture.completedFuture(new Result(Result.Status.NOT_INSTALLED, id, null, info.getVersion(), false, true));
                }
                if (! ModuleRegistry.updateAvailable(installed.getVersion(), installed.getJar(), info)) {
                    return CompletableFuture.completedFuture(new Result(Result.Status.UP_TO_DATE, installed.getId(),
                            installed.getVersion(), info.getVersion(), installed.isLoaded(), true));
                }
                return fetchAndInstallWorld(id, info.getVersion(), installed.getVersion());
            }
            ClientModuleManager.Installed installed = ClientModuleManager.module(id);
            if (installed == null) installed = ClientModuleManager.module(name);
            if (installed == null) {
                // A jar of it that did not load counts as installed: updating is how it gets fixed.
                String unloaded = ClientModuleManager.unloadedVersion(id);
                if (unloaded == null) {
                    return CompletableFuture.completedFuture(new Result(Result.Status.NOT_INSTALLED, id, null, info.getVersion(), false, false));
                }
                if (unloaded.equalsIgnoreCase(info.getVersion())
                        && ModuleRegistry.matchesRegistry(info, info.getVersion(), ClientModuleManager.unloadedJar(id))) {
                    throw new ModuleRegistry.RegistryException("'" + id + "' " + unloaded
                            + " is eCloud's latest version, and it did not load here. See /slclient modules list.");
                }
                return fetchAndInstall(id, info.getVersion(), unloaded);
            }
            if (! ModuleRegistry.updateAvailable(installed.getVersion(), installed.getJar(), info)) {
                return CompletableFuture.completedFuture(new Result(Result.Status.UP_TO_DATE, installed.getId(),
                        installed.getVersion(), info.getVersion(), installed.isEnabled(), false));
            }
            return fetchAndInstall(id, info.getVersion(), installed.getVersion());
        }, NETWORK).thenCompose(future -> future), "update", name);
    }

    /** Writes the outcome of an eCloud download or update to the client log. */
    private static CompletableFuture<Result> logged(CompletableFuture<Result> future, String action, String name) {
        return future.whenComplete((result, error) -> {
            if (error != null) {
                Throwable cause = error instanceof java.util.concurrent.CompletionException && error.getCause() != null
                        ? error.getCause() : error;
                String message = "eCloud " + action + " of '" + name + "' failed";
                // A registry refusal is a plain message for the player; anything else gets its stack trace.
                if (cause instanceof ModuleRegistry.RegistryException && cause.getCause() == null) {
                    singularity.client.ClientEnvironment.logWarning(message + ": " + cause.getMessage());
                } else {
                    singularity.client.ClientEnvironment.logWarning(message, cause);
                }
            } else if (result != null) {
                singularity.client.ClientEnvironment.logInfo("eCloud " + action + " of '" + name + "': "
                        + result.getStatus() + " " + result.getModuleId()
                        + (result.getOldVersion() == null ? "" : " " + result.getOldVersion() + " ->")
                        + " " + result.getVersion() + (result.isWorld() ? " (world module)" : ""));
            }
        });
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
                            installed.getId(), oldVersion, installed.getVersion(), installed.isEnabled(), false);
                });
    }

    private static CompletableFuture<Result> fetchAndInstallWorld(String id, String version, String oldVersion) {
        ModuleRegistry.Fetched fetched = REGISTRY.fetch(id, version, WorldModules.folder().toPath());
        return WorldModules.install(fetched)
                .handle((installed, error) -> {
                    if (error != null) {
                        Throwable cause = error.getCause() != null ? error.getCause() : error;
                        throw new ModuleRegistry.RegistryException(String.valueOf(cause.getMessage()), cause);
                    }
                    return new Result(oldVersion == null ? Result.Status.INSTALLED : Result.Status.UPDATED,
                            installed.getId(), oldVersion, installed.getVersion(), installed.isLoaded(), true);
                });
    }

    /** Whether the requested version (or the latest, for {@code null}) is a world module. */
    private static boolean worldVersion(ModuleRegistry.ModuleInfo info, String version) {
        if (version == null || version.isBlank()) return isWorldModule(info);
        ModuleRegistry.VersionInfo listed = info.version(version.trim());
        return WorldModules.isWorldModule(listed == null ? info.getPlatforms() : listed.getPlatforms());
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
        if (WorldModules.isWorldModule(platforms) && ! WorldModules.isSupported()) {
            throw new ModuleRegistry.RegistryException("'" + info.getName()
                    + "' runs in the worlds a client hosts, which this game does not run.");
        }
        if (! WorldModules.installable(platforms)) {
            throw new ModuleRegistry.RegistryException("'" + info.getName() + "' does not run on clients"
                    + (platforms.isEmpty() ? "." : " (it runs on " + String.join(", ", platforms) + ")."));
        }
        return info;
    }
}
