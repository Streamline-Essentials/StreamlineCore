package singularity.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.Singularity;
import singularity.interfaces.ISingularityExtension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for the Streamline module registry ("eCloud", {@code https://modules.drak.gg}).
 *
 * <p>Downloads land in the module folder under the file name the registry
 * suggests ({@code <Name>-<version>.jar}) and are verified against the
 * registry's SHA-256 before they are moved into place, so a truncated or
 * corrupted transfer never becomes a loadable jar.</p>
 */
public final class ModuleCloud {
    /** Registry root used when nothing else is configured. */
    public static final String DEFAULT_BASE_URL = "https://modules.drak.gg";

    /** How long a fetched module-name list is reused for tab completion. */
    private static final long NAME_CACHE_MILLIS = Duration.ofMinutes(5).toMillis();

    private static final Pattern FILENAME = Pattern.compile("filename=\"?([^\";]+)\"?");
    private static final Pattern SAFE_JAR_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*\\.jar");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Getter @Setter
    private static String baseUrl = DEFAULT_BASE_URL;

    /** How soon a failed module-name fetch is retried. */
    private static final long NAME_RETRY_MILLIS = Duration.ofSeconds(30).toMillis();

    /**
     * Runs module-name fetches. A daemon thread of its own keeps the request off
     * the common pool, which mods may saturate, and never holds up shutdown.
     */
    private static final Executor NAME_FETCHER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Streamline-eCloud");
        thread.setDaemon(true);
        return thread;
    });

    private static final ConcurrentSkipListSet<String> cachedNames = new ConcurrentSkipListSet<>();
    private static volatile long namesStaleAt = 0L;
    private static final AtomicBoolean namesRefreshing = new AtomicBoolean(false);

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

    /** A download that was refused or failed; the message is meant for the command sender. */
    public static final class CloudException extends RuntimeException {
        public CloudException(String message) {
            super(message);
        }

        public CloudException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Downloads a module from the registry into the module folder and loads it.
     * The transfer and checksum run off-thread; installing and loading the jar
     * run through {@link ISingularityExtension#runOnMainThread(Runnable)}.
     *
     * @param name    the module name or Plugin-Id, matched case-insensitively by the registry
     * @param version a specific version, or {@code null} for the latest
     * @return a future completed with the result, or exceptionally with a {@link CloudException}
     */
    public static CompletableFuture<Download> download(String name, String version) {
        return CompletableFuture.supplyAsync(() -> fetch(name, version))
                .thenCompose(fetched -> onMainThread(() -> install(fetched)));
    }

    /** A verified jar sitting in a temporary file in the module folder. */
    private static final class Fetched {
        final Path temp;
        final String moduleId;
        final String version;
        final String fileName;

        Fetched(Path temp, String moduleId, String version, String fileName) {
            this.temp = temp;
            this.moduleId = moduleId;
            this.version = version;
            this.fileName = fileName;
        }
    }

    private static Fetched fetch(String name, String version) {
        // "latest" is a registry keyword, never a real version.
        String target = version == null || version.isBlank() ? "latest" : version.trim();
        String url = apiUrl() + "/" + encode(name) + "/download/" + encode(target);

        Path folder = Singularity.getModuleFolder().toPath();
        Path temp;
        try {
            Files.createDirectories(folder);
            // Not a .jar, so the module scanner never picks up a partial download.
            temp = Files.createTempFile(folder, ".ecloud-", ".part");
        } catch (IOException e) {
            throw new CloudException("Could not write to the module folder: " + e.getMessage(), e);
        }

        boolean keep = false;
        try {
            HttpResponse<Path> response = send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(2))
                    .header("Accept", "application/java-archive")
                    .GET().build(), HttpResponse.BodyHandlers.ofFile(temp));

            if (response.statusCode() != 200) {
                throw new CloudException(errorMessage(response.statusCode(), Files.readString(temp)));
            }

            String moduleId = response.headers().firstValue("X-Module-Id").orElse(name);
            String moduleVersion = response.headers().firstValue("X-Module-Version").orElse(target);
            String expected = response.headers().firstValue("X-Checksum-SHA256").orElse(null);
            if (expected != null && ! expected.equalsIgnoreCase(sha256(temp))) {
                throw new CloudException("Checksum mismatch for '" + moduleId + "'; the download was discarded.");
            }

            String fileName = response.headers().firstValue("Content-Disposition")
                    .map(FILENAME::matcher)
                    .filter(Matcher::find)
                    .map(m -> m.group(1).trim())
                    .filter(n -> SAFE_JAR_NAME.matcher(n).matches())
                    .orElse(name + "-" + moduleVersion + ".jar");

            keep = true;
            return new Fetched(temp, moduleId, moduleVersion, fileName);
        } catch (IOException e) {
            throw new CloudException("Could not reach " + baseUrl() + ": " + e.getMessage(), e);
        } finally {
            if (! keep) deleteQuietly(temp);
        }
    }

    private static Download install(Fetched fetched) {
        try {
            PluginWrapper installed = ModuleManager.safePluginManager().getPlugin(fetched.moduleId);
            if (installed != null) {
                String installedVersion = installed.getDescriptor().getVersion();
                String installedJar = installed.getPluginPath().getFileName().toString();
                if (installedVersion.equals(fetched.version)) {
                    throw new CloudException("'" + fetched.moduleId + "' " + fetched.version + " is already installed (" + installedJar + ").");
                }
                // PF4J identifies modules by Plugin-Id, so a second jar with the same id
                // would clash on the next start; replacing a loaded jar needs a restart.
                throw new CloudException("'" + fetched.moduleId + "' " + installedVersion + " is already installed (" + installedJar
                        + "). Delete that jar and restart to switch to " + fetched.version + ".");
            }

            // Jars PF4J has not loaded (unloaded, or failed to load) still claim their
            // Plugin-Id on the next start. A jar with the target name is simply replaced.
            String clash = jarOnDiskWithId(fetched.moduleId, fetched.fileName);
            if (clash != null) {
                throw new CloudException("'" + clash + "' in the module folder already has the Plugin-Id '"
                        + fetched.moduleId + "'. Delete it first.");
            }

            Path target = Singularity.getModuleFolder().toPath().resolve(fetched.fileName);
            try {
                move(fetched.temp, target);
            } catch (IOException e) {
                throw new CloudException("Could not save " + fetched.fileName + ": " + e.getMessage(), e);
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
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A leftover .part file is harmless; it is never loaded.
        }
    }

    /**
     * Module names known to the registry, for tab completion. Never blocks:
     * returns the cached set and, when it is stale, refreshes it in the
     * background. {@link #refreshModuleNames()} fills it at start-up.
     */
    public static ConcurrentSkipListSet<String> getCachedModuleNames() {
        if (System.currentTimeMillis() >= namesStaleAt) refreshModuleNames();
        return cachedNames;
    }

    /**
     * Fetches the registry's module names in the background, unless a fetch is
     * already running. A failed fetch is retried after {@link #NAME_RETRY_MILLIS}
     * rather than the full cache period, so a registry that was unreachable at
     * start-up does not leave completion empty for long.
     */
    public static void refreshModuleNames() {
        if (! namesRefreshing.compareAndSet(false, true)) return;
        try {
            NAME_FETCHER.execute(ModuleCloud::fetchModuleNames);
        } catch (RuntimeException e) {
            namesRefreshing.set(false);
        }
    }

    private static void fetchModuleNames() {
        long nextRefresh = NAME_RETRY_MILLIS;
        try {
            HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(apiUrl() + "/modules"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;

            ConcurrentSkipListSet<String> names = new ConcurrentSkipListSet<>();
            for (JsonElement element : new JsonParser().parse(response.body()).getAsJsonObject().getAsJsonArray("modules")) {
                names.add(element.getAsJsonObject().get("name").getAsString());
            }
            cachedNames.retainAll(names);
            cachedNames.addAll(names);
            nextRefresh = NAME_CACHE_MILLIS;
        } catch (Exception ignored) {
            // Completion simply keeps the previous list.
        } finally {
            namesStaleAt = System.currentTimeMillis() + nextRefresh;
            namesRefreshing.set(false);
        }
    }

    private static <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            return CLIENT.send(request, handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted", e);
        }
    }

    private static String baseUrl() {
        String url = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String apiUrl() {
        return baseUrl() + "/api/v1";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** The registry answers errors as {@code {"error": "..."}}; falls back to the status code. */
    private static String errorMessage(int status, String body) {
        try {
            JsonObject json = new JsonParser().parse(body).getAsJsonObject();
            if (json.has("error")) return json.get("error").getAsString();
        } catch (Exception ignored) {
            // Not JSON (e.g. a proxy error page).
        }
        return "The module registry answered HTTP " + status + ".";
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) hex.append(String.format(Locale.ROOT, "%02x", b));
        return hex.toString();
    }
}
