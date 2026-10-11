package singularity.client.module;

import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginWrapper;
import singularity.client.ClientEnvironment;
import singularity.loading.ModuleClassTransformer;
import singularity.registry.ModuleRegistry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Loads the game client's modules from {@code client/modules/} and swaps them while the game
 * runs: a jar dropped into the folder loads, a jar replaced reloads, a jar deleted unloads,
 * and the menu, {@code /slclient modules} and the eCloud browser do the same on request.
 *
 * <p>Every operation runs on one daemon thread, so loads, unloads and folder scans never
 * overlap and never hold up the render thread. Each jar is loaded from a private copy in
 * {@code client/.module-cache/}: the class loader keeps that copy open, so the jar in the
 * module folder stays free to be replaced or deleted (Windows refuses both while a jar is
 * open), and a reload never reads stale bytes from the JVM's jar cache, which is keyed by
 * path.</p>
 *
 * <p>Only jars whose {@code Plugin-Platforms} include {@code modded-client} but not
 * {@code modded-server}, and whose main class is a {@link ClientModule}, are loaded. Jars
 * with both tags are server modules that also run on the worlds a client hosts, so they are
 * moved to the server module folder ({@link WorldModules}); anything else is refused with a
 * reason, shown by {@link #refusals()}, until the jar changes.</p>
 */
public final class ClientModuleManager {

    /** The platform tag a jar must carry to load on a game client. */
    public static final String CLIENT_TAG = "modded-client";

    /** How often the module folder is checked for added, replaced and deleted jars. */
    private static final long SCAN_MILLIS = 2000L;
    /** A jar modified more recently than this may still be being written, so it waits. */
    private static final long SETTLE_MILLIS = 1000L;

    private static final ScheduledExecutorService WORKER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Streamline-ClientModules");
        thread.setDaemon(true);
        return thread;
    });

    /** Loaded modules by Plugin-Id; written only on {@link #WORKER}. */
    private static final Map<String, Entry> LOADED = new ConcurrentHashMap<>();
    /** Jars that were refused, by file name, until they change. */
    private static final Map<String, Refusal> REFUSED = new ConcurrentHashMap<>();
    private static final AtomicLong COPY_COUNTER = new AtomicLong();

    private static volatile ClientPluginManager pluginManager;
    private static volatile boolean started;
    private static volatile boolean stopped;
    private static volatile ModuleClassTransformer classTransformer;

    private ClientModuleManager() {}

    /**
     * Rewrites client module classes before they are defined, or {@code null} to define them
     * as they are in the jar. Minecraft 1.8.9 sets one that rewrites them for Java 8. Applies
     * to modules loaded after it is set.
     */
    public static void setClassTransformer(ModuleClassTransformer transformer) {
        classTransformer = transformer;
    }

    /** @return the transformer client module classes go through, or {@code null} */
    public static ModuleClassTransformer getClassTransformer() {
        return classTransformer;
    }

    /** A loaded module and the jar it came from. */
    private static final class Entry {
        final String id;
        final Path jar;
        final Path copy;
        final long modified;
        final long size;
        final ClientModule module;

        Entry(String id, Path jar, Path copy, long modified, long size, ClientModule module) {
            this.id = id;
            this.jar = jar;
            this.copy = copy;
            this.modified = modified;
            this.size = size;
            this.module = module;
        }
    }

    private static final class Refusal {
        final long modified;
        final long size;
        final String reason;

        Refusal(long modified, long size, String reason) {
            this.modified = modified;
            this.size = size;
            this.reason = reason;
        }
    }

    /** What the menu and {@code /slclient modules list} show for a loaded module. */
    public static final class Installed {
        private final ClientModule module;
        private final String fileName;

        Installed(ClientModule module, String fileName) {
            this.module = module;
            this.fileName = fileName;
        }

        public ClientModule getModule() {
            return module;
        }

        public String getId() {
            return module.getIdentifier();
        }

        public String getVersion() {
            return module.getVersion();
        }

        public String getDescription() {
            String description = module.getDescriptor().getPluginDescription();
            return description == null ? "" : description;
        }

        public String getProvider() {
            String provider = module.getDescriptor().getProvider();
            return provider == null ? "" : provider;
        }

        /** The main class's simple name, which by convention is the module's name. */
        public String getName() {
            String type = module.getDescriptor().getPluginClass();
            return type == null ? getId() : type.substring(type.lastIndexOf('.') + 1);
        }

        public boolean isEnabled() {
            return module.isEnabled();
        }

        public String getFileName() {
            return fileName;
        }
    }

    // ---------------------------------------------------------------- folders

    /** {@code client/modules/}, where client module jars go, or {@code null} off a client. */
    public static File folder() {
        return ClientEnvironment.file("modules");
    }

    private static File cacheFolder() {
        return ClientEnvironment.file(".module-cache");
    }

    private static File stateFile() {
        return ClientEnvironment.file("modules.properties");
    }

    // ---------------------------------------------------------------- lifecycle

    /**
     * Loads and enables the client modules, then watches the module folder. Needs
     * {@link ClientEnvironment#setFolder} first. Runs once; later calls do nothing.
     */
    public static synchronized void start() {
        if (started || stopped) return;
        File folder = folder();
        if (folder == null) {
            ClientEnvironment.logWarning("Client modules are off: the client folder is not set.");
            return;
        }
        started = true;
        submit(() -> {
            folder.mkdirs();
            clearCache();
            adoptLegacyJars();
            Path cache = cacheFolder().toPath();
            Files.createDirectories(cache);
            pluginManager = new ClientPluginManager(cache);
            scan(true);
            return null;
        });
        WORKER.scheduleWithFixedDelay(() -> {
            try {
                scan(false);
            } catch (Throwable t) {
                ClientEnvironment.logWarning("Could not check the client module folder: " + t);
            }
        }, SCAN_MILLIS, SCAN_MILLIS, TimeUnit.MILLISECONDS);
    }

    /** Disables and unloads every client module; the game is exiting. */
    public static void shutdown() {
        synchronized (ClientModuleManager.class) {
            if (stopped) return;
            stopped = true;
        }
        if (! started) return;
        try {
            submit(() -> {
                for (Entry entry : new ArrayList<>(LOADED.values())) unload(entry);
                return null;
            }).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            ClientEnvironment.logWarning("Client modules did not all stop: " + e);
        }
        WORKER.shutdownNow();
    }

    /** @return whether client modules are running, i.e. this is a game client */
    public static boolean isStarted() {
        return started && ! stopped;
    }

    // ---------------------------------------------------------------- queries

    /** The loaded client modules, sorted by id. Safe from any thread. */
    public static List<Installed> modules() {
        List<Installed> result = new ArrayList<>();
        for (Entry entry : new TreeMap<>(LOADED).values()) {
            result.add(new Installed(entry.module, entry.jar.getFileName().toString()));
        }
        return result;
    }

    /** The loaded module with this Plugin-Id, ignoring case, or {@code null}. */
    public static Installed module(String id) {
        Entry entry = entry(id);
        return entry == null ? null : new Installed(entry.module, entry.jar.getFileName().toString());
    }

    /** Jars in the module folder that were not loaded, by file name, with the reason. */
    public static Map<String, String> refusals() {
        Map<String, String> result = new TreeMap<>();
        REFUSED.forEach((name, refusal) -> result.put(name, refusal.reason));
        return result;
    }

    private static Entry entry(String id) {
        if (id == null) return null;
        Entry exact = LOADED.get(id);
        if (exact != null) return exact;
        for (Entry entry : LOADED.values()) {
            if (entry.id.equalsIgnoreCase(id)) return entry;
        }
        return null;
    }

    // ---------------------------------------------------------------- operations

    /** Enables a loaded module and remembers that it is on. */
    public static CompletableFuture<Installed> enable(String id) {
        return submit(() -> {
            Entry entry = require(id);
            remember(entry.id, true);
            entry.module.start();
            return module(entry.id);
        });
    }

    /** Disables a loaded module, keeping it loaded, and remembers that it is off. */
    public static CompletableFuture<Installed> disable(String id) {
        return submit(() -> {
            Entry entry = require(id);
            remember(entry.id, false);
            entry.module.stop();
            return module(entry.id);
        });
    }

    /** Unloads a module and loads its jar again, with the modules that depend on it. */
    public static CompletableFuture<Installed> reload(String id) {
        return submit(() -> {
            Entry entry = require(id);
            List<Path> again = unloadWithDependents(entry);
            for (Path jar : again) load(jar);
            Installed reloaded = module(entry.id);
            if (reloaded == null) throw new IllegalStateException(refusalFor(entry.jar));
            return reloaded;
        });
    }

    /**
     * Unloads a module and deletes its jar. Its data folder, settings and HUD layout stay,
     * so installing it again picks up where it left off.
     *
     * @return the deleted jar's file name
     */
    public static CompletableFuture<String> remove(String id) {
        return submit(() -> {
            Entry entry = entry(id);
            if (entry == null) {
                // A jar that did not load (built for an older client, say) can be removed too.
                Path jar = jarWithId(id, null);
                if (jar == null) throw new IllegalArgumentException("No client module '" + id + "' is installed.");
                deleteOrDefer(jar);
                REFUSED.remove(jar.getFileName().toString());
                return jar.getFileName().toString();
            }
            List<Path> dependents = unloadWithDependents(entry);
            dependents.remove(entry.jar);
            deleteOrDefer(entry.jar);
            REFUSED.remove(entry.jar.getFileName().toString());
            for (Path jar : dependents) load(jar);
            return entry.jar.getFileName().toString();
        });
    }

    /**
     * Moves a verified download into the module folder and loads it, replacing the loaded
     * module with the same Plugin-Id if there is one.
     *
     * @return the module as loaded
     */
    public static CompletableFuture<Installed> install(ModuleRegistry.Fetched fetched) {
        return submit(() -> {
            try {
                Path temp = fetched.getTemp();
                if (! WorldModules.isClientModule(ModuleRegistry.manifestPlatforms(temp))) {
                    throw new IllegalStateException("'" + fetched.getModuleId() + "' is not a client module.");
                }
                String id = ModuleRegistry.manifestValue(temp, "Plugin-Id");
                if (id == null) id = fetched.getModuleId();

                Path target = folder().toPath().resolve(fetched.getFileName());
                Entry old = entry(id);
                List<Path> dependents = new ArrayList<>();
                if (old != null) {
                    dependents = unloadWithDependents(old);
                    dependents.remove(old.jar);
                    if (! old.jar.equals(target)) deleteOrDefer(old.jar);
                } else {
                    // A jar with this id that is not loaded was refused (or is still being
                    // written); the player asked for this module, so the download replaces it.
                    Path stale = jarWithId(id, target);
                    if (stale != null) {
                        deleteOrDefer(stale);
                        REFUSED.remove(stale.getFileName().toString());
                    }
                }

                ModuleRegistry.move(temp, target);
                REFUSED.remove(target.getFileName().toString());
                load(target);
                for (Path jar : dependents) load(jar);

                Installed installed = module(id);
                if (installed == null) throw new IllegalStateException(refusalFor(target));
                return installed;
            } finally {
                fetched.discard();
            }
        });
    }

    private static Entry require(String id) {
        Entry entry = entry(id);
        if (entry == null) throw new IllegalArgumentException("No client module '" + id + "' is loaded.");
        return entry;
    }

    private static String refusalFor(Path jar) {
        Refusal refusal = REFUSED.get(jar.getFileName().toString());
        return refusal == null ? "'" + jar.getFileName() + "' did not load." : refusal.reason;
    }

    // ---------------------------------------------------------------- loading

    /**
     * Brings the loaded modules in line with the module folder.
     *
     * @param initial whether this is the first scan, which loads every jar at once and so
     *                retries jars whose dependencies load later in the same pass
     */
    private static void scan(boolean initial) throws IOException {
        if (stopped || pluginManager == null) return;
        Path folder = folder().toPath();
        Map<String, Path> jars = new TreeMap<>();
        if (Files.isDirectory(folder)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.jar")) {
                for (Path jar : stream) jars.put(jar.getFileName().toString(), jar);
            }
        }

        // Deleted or replaced jars first, so a replaced jar is free to load again below.
        for (Entry entry : new ArrayList<>(LOADED.values())) {
            if (! LOADED.containsKey(entry.id)) continue;
            Path jar = jars.get(entry.jar.getFileName().toString());
            if (jar == null) {
                ClientEnvironment.logInfo("Client module '" + entry.id + "' was removed from the folder; unloading it.");
                for (Path dependent : unloadWithDependents(entry)) {
                    if (! dependent.equals(entry.jar)) load(dependent);
                }
            } else if (! settled(jar)) {
                jars.remove(jar.getFileName().toString());
            } else if (changed(jar, entry.modified, entry.size)) {
                ClientEnvironment.logInfo("Client module jar '" + jar.getFileName() + "' changed; reloading it.");
                for (Path again : unloadWithDependents(entry)) load(again);
            }
        }

        REFUSED.keySet().retainAll(jars.keySet());

        List<Path> pending = new ArrayList<>();
        List<Path> refused = new ArrayList<>();
        for (Path jar : jars.values()) {
            if (isLoaded(jar) || ! settled(jar)) continue;
            Refusal refusal = REFUSED.get(jar.getFileName().toString());
            if (refusal != null && ! changed(jar, refusal.modified, refusal.size)) refused.add(jar);
            else pending.add(jar);
        }

        // A module can need another that sorts after it, or that arrives later: whenever a pass
        // loads something, the jars still waiting, refused ones included, get another try.
        boolean progress = loadAll(pending);
        if (initial || progress) {
            pending.addAll(refused);
            while (progress && ! pending.isEmpty()) progress = loadAll(pending);
        }
    }

    /** Tries each jar once, dropping those that load. @return whether any loaded */
    private static boolean loadAll(List<Path> jars) {
        boolean progress = false;
        for (Path jar : new ArrayList<>(jars)) {
            load(jar);
            if (isLoaded(jar)) {
                jars.remove(jar);
                progress = true;
            }
        }
        return progress;
    }

    private static boolean isLoaded(Path jar) {
        for (Entry entry : LOADED.values()) {
            if (entry.jar.equals(jar)) return true;
        }
        return false;
    }

    private static boolean settled(Path jar) {
        try {
            return System.currentTimeMillis() - Files.getLastModifiedTime(jar).toMillis() >= SETTLE_MILLIS;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean changed(Path jar, long modified, long size) {
        try {
            return Files.getLastModifiedTime(jar).toMillis() != modified || Files.size(jar) != size;
        } catch (IOException e) {
            return true;
        }
    }

    /** Loads one jar and enables its module unless the player turned it off. Refusals are recorded, not thrown. */
    private static void load(Path jar) {
        String name = jar.getFileName().toString();
        long modified;
        long size;
        try {
            modified = Files.getLastModifiedTime(jar).toMillis();
            size = Files.size(jar);
        } catch (IOException e) {
            return;
        }

        List<String> platforms = ModuleRegistry.manifestPlatforms(jar);
        if (WorldModules.isWorldModule(platforms)) {
            if (WorldModules.adopt(jar)) return;
            refuse(name, modified, size, WorldModules.isSupported()
                    ? "'" + name + "' runs in the worlds this client hosts; move it to " + WorldModules.folder() + "."
                    : "'" + name + "' runs only in worlds a client hosts, which this game does not run.");
            return;
        }
        if (! WorldModules.isClientModule(platforms)) {
            refuse(name, modified, size, platforms.isEmpty()
                    ? "'" + name + "' declares no platforms, so it is not a client module."
                    : "'" + name + "' is not a client module (platforms: " + String.join(", ", platforms) + ").");
            return;
        }
        String id = ModuleRegistry.manifestValue(jar, "Plugin-Id");
        if (id == null || id.isEmpty()) {
            refuse(name, modified, size, "'" + name + "' has no Plugin-Id.");
            return;
        }
        Entry existing = entry(id);
        if (existing != null) {
            refuse(name, modified, size, "'" + name + "' has the Plugin-Id '" + id + "', which '"
                    + existing.jar.getFileName() + "' already loaded.");
            return;
        }

        Path copy = cacheFolder().toPath().resolve(id.replaceAll("[^A-Za-z0-9._-]", "_")
                + "-" + COPY_COUNTER.incrementAndGet() + ".jar");
        String loadedId = null;
        try {
            Files.copy(jar, copy, StandardCopyOption.REPLACE_EXISTING);
            loadedId = pluginManager.loadPlugin(copy);
            if (loadedId == null) throw new IllegalStateException("PF4J did not load it");

            PluginWrapper wrapper = pluginManager.getPlugin(loadedId);
            PluginDescriptor descriptor = wrapper.getDescriptor();
            String fix = " It was built for an older Streamline client. Update it with /slclient modules ecloud update "
                    + loadedId + ", or remove it with /slclient modules remove " + loadedId + ".";
            Class<?> type;
            try {
                type = wrapper.getPluginClassLoader().loadClass(descriptor.getPluginClass());
            } catch (LinkageError e) {
                // Built against a class the client does not have, such as SimpleModule.
                throw new IllegalStateException("it needs " + e.getMessage().replace('/', '.')
                        + ", which a game client does not have." + fix, e);
            }
            if (! ClientModule.class.isAssignableFrom(type)) {
                throw new IllegalStateException("its main class " + type.getSimpleName() + " is not a ClientModule." + fix);
            }

            ClientModule module = (ClientModule) wrapper.getPlugin();
            if (module == null) throw new IllegalStateException("its main class could not be created; see the log");
            module.onLoad();

            Entry entry = new Entry(loadedId, jar, copy, modified, size, module);
            LOADED.put(loadedId, entry);
            REFUSED.remove(name);

            if (isWanted(loadedId)) {
                try {
                    module.start();
                } catch (Throwable t) {
                    ClientEnvironment.logWarning("Client module '" + loadedId + "' failed to enable: " + t);
                }
            }
            ClientEnvironment.logInfo("Loaded client module '" + loadedId + "' " + descriptor.getVersion()
                    + (module.isEnabled() ? "" : " (disabled)") + " from " + name + ".");
        } catch (Throwable t) {
            // PF4J registers a plugin before resolving its dependencies, and keeps it when
            // that fails, which would block every later attempt with "already loaded".
            String registered = loadedId != null ? loadedId : id;
            if (! LOADED.containsKey(registered)) {
                pluginManager.forget(registered);
                deleteCopy(copy);
            }
            String reason = t.getMessage() == null ? t.toString() : t.getMessage();
            refuse(name, modified, size, "'" + name + "' did not load: " + reason);
        }
    }

    private static void refuse(String name, long modified, long size, String reason) {
        Refusal previous = REFUSED.put(name, new Refusal(modified, size, reason));
        if (previous == null || ! previous.reason.equals(reason)) ClientEnvironment.logWarning(reason);
    }

    /**
     * Unloads a module after the modules that depend on it.
     *
     * @return the jars unloaded, the module's own first, so they can be loaded again in order
     */
    private static List<Path> unloadWithDependents(Entry entry) {
        List<Path> jars = new ArrayList<>();
        jars.add(entry.jar);
        for (Entry dependent : dependentsOf(entry.id)) {
            for (Path jar : unloadWithDependents(dependent)) {
                if (! jars.contains(jar)) jars.add(jar);
            }
        }
        unload(entry);
        return jars;
    }

    private static List<Entry> dependentsOf(String id) {
        List<Entry> dependents = new ArrayList<>();
        for (Entry entry : LOADED.values()) {
            for (PluginDependency dependency : entry.module.getDescriptor().getDependencies()) {
                if (dependency.getPluginId().equalsIgnoreCase(id)) {
                    dependents.add(entry);
                    break;
                }
            }
        }
        return dependents;
    }

    private static void unload(Entry entry) {
        if (LOADED.remove(entry.id) == null) return;
        try {
            entry.module.stop();
        } catch (Throwable t) {
            ClientEnvironment.logWarning("Client module '" + entry.id + "' failed to disable: " + t);
        }
        // Closes the module's class loader, which releases the copy it was loaded from.
        pluginManager.forget(entry.id);
        deleteCopy(entry.copy);
        ClientEnvironment.logInfo("Unloaded client module '" + entry.id + "'.");
    }

    // ---------------------------------------------------------------- files

    private static void deleteCopy(Path copy) {
        try {
            Files.deleteIfExists(copy);
        } catch (IOException e) {
            // Still open somewhere; the cache is cleared at the next start.
            copy.toFile().deleteOnExit();
        }
    }

    private static void deleteOrDefer(Path jar) {
        try {
            Files.deleteIfExists(jar);
        } catch (IOException e) {
            jar.toFile().deleteOnExit();
            ClientEnvironment.logWarning("Could not delete " + jar.getFileName() + " now; it is deleted when the game exits.");
        }
    }

    private static void clearCache() {
        File cache = cacheFolder();
        File[] files = cache == null ? null : cache.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (! file.delete()) file.deleteOnExit();
        }
    }

    /** A jar in the module folder, other than {@code except}, with this Plugin-Id, ignoring case. */
    private static Path jarWithId(String id, Path except) throws IOException {
        File folderFile = folder();
        if (folderFile == null || ! folderFile.isDirectory()) return null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderFile.toPath(), "*.jar")) {
            for (Path jar : stream) {
                if (jar.equals(except)) continue;
                if (id.equalsIgnoreCase(ModuleRegistry.manifestValue(jar, "Plugin-Id"))) return jar;
            }
        }
        return null;
    }

    /**
     * The version of a client module jar in the folder that did not load, by Plugin-Id, or
     * {@code null} when there is none. Lets an update replace a jar that was refused.
     */
    public static String unloadedVersion(String id) {
        if (entry(id) != null) return null;
        try {
            Path jar = jarWithId(id, null);
            if (jar == null) return null;
            String version = ModuleRegistry.manifestValue(jar, "Plugin-Version");
            return version == null ? "0" : version;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Moves client-only jars out of Streamline's server module folder ({@code modules/},
     * beside {@code client/}), where client modules were kept before they had a folder of
     * their own. Jars that also run on servers stay where they are.
     */
    private static void adoptLegacyJars() {
        File clientFolder = ClientEnvironment.folder();
        File serverModules = clientFolder == null ? null : new File(clientFolder.getParentFile(), "modules");
        File[] jars = serverModules == null ? null : serverModules.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null) return;
        for (File jar : jars) {
            if (! WorldModules.isClientModule(ModuleRegistry.manifestPlatforms(jar.toPath()))) continue;
            Path target = folder().toPath().resolve(jar.getName());
            if (Files.exists(target)) continue;
            try {
                Files.move(jar.toPath(), target);
                ClientEnvironment.logInfo("Moved client module " + jar.getName() + " to " + folder() + ".");
            } catch (IOException e) {
                ClientEnvironment.logWarning("Could not move client module " + jar.getName() + " to " + folder() + ": " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------- on/off state

    private static synchronized Properties state() {
        Properties properties = new Properties();
        File file = stateFile();
        if (file == null || ! file.isFile()) return properties;
        try (InputStream in = new FileInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            ClientEnvironment.logWarning("Could not read " + file + ": " + e.getMessage());
        }
        return properties;
    }

    private static boolean isWanted(String id) {
        return ! "false".equalsIgnoreCase(state().getProperty(id));
    }

    private static synchronized void remember(String id, boolean enabled) {
        File file = stateFile();
        if (file == null) return;
        Properties properties = state();
        if (enabled) properties.remove(id);
        else properties.setProperty(id, "false");
        file.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(file)) {
            properties.store(out, "Client modules turned off in the Streamline client menu: <module-id> = false");
        } catch (IOException e) {
            ClientEnvironment.logWarning("Could not write " + file + ": " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- worker

    private static <T> CompletableFuture<T> submit(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            WORKER.execute(() -> {
                try {
                    future.complete(task.call());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (RuntimeException e) {
            future.completeExceptionally(new IllegalStateException("Client modules have stopped."));
        }
        return future;
    }
}
