package singularity.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.Singularity;

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
import java.util.concurrent.atomic.AtomicBoolean;
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

    private static final ConcurrentSkipListSet<String> cachedNames = new ConcurrentSkipListSet<>();
    private static volatile long namesFetchedAt = 0L;
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
     *
     * @param name    the module name or Plugin-Id, matched case-insensitively by the registry
     * @param version a specific version, or {@code null} for the latest
     * @return a future completed with the result, or exceptionally with a {@link CloudException}
     */
    public static CompletableFuture<Download> download(String name, String version) {
        return CompletableFuture.supplyAsync(() -> downloadBlocking(name, version));
    }

    private static Download downloadBlocking(String name, String version) {
        String url = moduleUrl(name) + "/download";
        if (version != null && ! version.isBlank()) url += "?version=" + encode(version);

        Path folder = Singularity.getModuleFolder().toPath();
        Path temp;
        try {
            Files.createDirectories(folder);
            // Not a .jar, so the module scanner never picks up a partial download.
            temp = Files.createTempFile(folder, ".ecloud-", ".part");
        } catch (IOException e) {
            throw new CloudException("Could not write to the module folder: " + e.getMessage(), e);
        }

        try {
            HttpResponse<Path> response = send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(2))
                    .header("Accept", "application/java-archive")
                    .GET().build(), HttpResponse.BodyHandlers.ofFile(temp));

            if (response.statusCode() != 200) {
                throw new CloudException(errorMessage(response.statusCode(), Files.readString(temp)));
            }

            String moduleId = response.headers().firstValue("X-Module-Id").orElse(name);
            String moduleVersion = response.headers().firstValue("X-Module-Version").orElse(version);
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

            PluginWrapper installed = ModuleManager.safePluginManager().getPlugin(moduleId);
            if (installed != null) {
                String installedVersion = installed.getDescriptor().getVersion();
                String installedJar = installed.getPluginPath().getFileName().toString();
                if (installedVersion.equals(moduleVersion)) {
                    throw new CloudException("'" + moduleId + "' " + moduleVersion + " is already installed (" + installedJar + ").");
                }
                // PF4J identifies modules by Plugin-Id, so a second jar with the same id
                // would clash on the next start; replacing a loaded jar needs a restart.
                throw new CloudException("'" + moduleId + "' " + installedVersion + " is already installed (" + installedJar
                        + "). Delete that jar and restart to switch to " + moduleVersion + ".");
            }

            Path target = folder.resolve(fileName);
            move(temp, target);

            try {
                ModuleManager.registerExternalModule(fileName);
            } catch (Throwable e) {
                return new Download(moduleId, moduleVersion, target, false, String.valueOf(e.getMessage()));
            }
            boolean loaded = ModuleManager.getPluginWrapperByJarName(fileName) != null;
            return new Download(moduleId, moduleVersion, target, loaded, loaded ? null : "PF4J did not register the jar");
        } catch (IOException e) {
            throw new CloudException("Could not reach " + baseUrl + ": " + e.getMessage(), e);
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // A leftover .part file is harmless; it is never loaded.
            }
        }
    }

    /**
     * Module names known to the registry, for tab completion. Returns the cached
     * set immediately and refreshes it in the background when it is stale, so
     * the first completion after start-up may be empty.
     */
    public static ConcurrentSkipListSet<String> getCachedModuleNames() {
        if (System.currentTimeMillis() - namesFetchedAt > NAME_CACHE_MILLIS && namesRefreshing.compareAndSet(false, true)) {
            CompletableFuture.runAsync(() -> {
                try {
                    HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(baseUrl() + "/api/v1/modules"))
                            .timeout(Duration.ofSeconds(15))
                            .header("Accept", "application/json")
                            .GET().build(), HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200) return;

                    ConcurrentSkipListSet<String> names = new ConcurrentSkipListSet<>();
                    for (JsonElement element : JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("modules")) {
                        names.add(element.getAsJsonObject().get("name").getAsString());
                    }
                    cachedNames.retainAll(names);
                    cachedNames.addAll(names);
                } catch (Exception ignored) {
                    // Completion simply keeps the previous list.
                } finally {
                    namesFetchedAt = System.currentTimeMillis();
                    namesRefreshing.set(false);
                }
            });
        }
        return cachedNames;
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

    private static String moduleUrl(String name) {
        return baseUrl() + "/api/v1/modules/" + encode(name);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** The registry answers errors as {@code {"error": "..."}}; falls back to the status code. */
    private static String errorMessage(int status, String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
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
