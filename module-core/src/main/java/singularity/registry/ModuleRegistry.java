package singularity.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP client for the Streamline module registry ("eCloud", {@code https://modules.drak.gg}),
 * shared by the server's {@code /modules ecloud} and the game client's module browser.
 *
 * <p>It only talks to the registry and verifies what it downloads. Installing and loading a
 * jar is up to the caller, since servers and clients keep modules in different places and
 * load them differently.</p>
 */
public final class ModuleRegistry {
    /** Registry root used when nothing else is configured. */
    public static final String DEFAULT_BASE_URL = "https://modules.drak.gg";

    private static final Pattern FILENAME = Pattern.compile("filename=\"?([^\";]+)\"?");
    private static final Pattern SAFE_JAR_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*\\.jar");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final Supplier<String> baseUrl;

    /** @param baseUrl supplies the registry root on every request, so it can be reconfigured */
    public ModuleRegistry(Supplier<String> baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** A registry at {@link #DEFAULT_BASE_URL}. */
    public ModuleRegistry() {
        this(() -> DEFAULT_BASE_URL);
    }

    /** A request that was refused or failed; the message is meant for whoever asked. */
    public static final class RegistryException extends RuntimeException {
        public RegistryException(String message) {
            super(message);
        }

        public RegistryException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** One version of a module, as the registry lists it. */
    @Getter
    public static final class VersionInfo {
        private final String version;
        private final long size;
        private final String sha256;
        private final String uploadedAt;
        private final long downloads;
        /** The platform tags this version's jar declares. */
        private final List<String> platforms;

        VersionInfo(JsonObject json) {
            this.version = string(json, "version");
            this.size = number(json, "size");
            this.sha256 = string(json, "sha256");
            this.uploadedAt = string(json, "uploadedAt");
            this.downloads = number(json, "downloads");
            this.platforms = strings(json, "platforms");
        }

        public boolean runsOn(String platform) {
            return containsIgnoreCase(platforms, platform);
        }
    }

    /** A module's registry entry: its metadata, total downloads and versions, newest first. */
    @Getter
    public static final class ModuleInfo {
        private final String name;
        private final String id;
        private final String description;
        private final String version;
        private final String author;
        private final String license;
        private final String requires;
        private final List<String> dependencies;
        /** The platform tags of the latest version. */
        private final List<String> platforms;
        private final long size;
        private final String sha256;
        private final String createdAt;
        private final String updatedAt;
        private final long downloads;
        private final String downloadUrl;
        private final List<VersionInfo> versions;

        ModuleInfo(JsonObject json) {
            this.name = string(json, "name");
            this.id = string(json, "id");
            this.description = string(json, "description");
            this.version = string(json, "version");
            this.author = string(json, "author");
            this.license = string(json, "license");
            this.requires = string(json, "requires");
            this.dependencies = new ArrayList<>();
            if (json.has("dependencies") && json.get("dependencies").isJsonArray()) {
                for (JsonElement element : json.getAsJsonArray("dependencies")) {
                    if (element.isJsonPrimitive()) {
                        dependencies.add(element.getAsString());
                    } else if (element.isJsonObject()) {
                        JsonObject dependency = element.getAsJsonObject();
                        String depId = dependency.has("id") ? string(dependency, "id") : string(dependency, "name");
                        if (! depId.isEmpty()) dependencies.add(depId);
                    }
                }
            }
            this.platforms = strings(json, "platforms");
            this.size = number(json, "size");
            this.sha256 = string(json, "sha256");
            this.createdAt = string(json, "createdAt");
            this.updatedAt = string(json, "updatedAt");
            this.downloads = number(json, "downloads");
            this.downloadUrl = string(json, "downloadUrl");
            this.versions = new ArrayList<>();
            if (json.has("versions") && json.get("versions").isJsonArray()) {
                for (JsonElement element : json.getAsJsonArray("versions")) {
                    if (element.isJsonObject()) versions.add(new VersionInfo(element.getAsJsonObject()));
                }
            }
        }

        /** @return whether the latest version runs on {@code platform} (a platform tag id) */
        public boolean runsOn(String platform) {
            return containsIgnoreCase(platforms, platform);
        }

        /** @return the listed version with that name, ignoring case, or {@code null} */
        public VersionInfo version(String name) {
            for (VersionInfo info : versions) {
                if (info.getVersion().equalsIgnoreCase(name)) return info;
            }
            return null;
        }
    }

    /** A verified jar the registry sent, sitting in a temporary {@code .part} file. */
    @Getter
    public static final class Fetched {
        /** The downloaded jar. Not named {@code .jar}, so no module scanner picks it up. */
        private final Path temp;
        private final String moduleId;
        private final String version;
        /** The file name the registry suggests, checked to be a plain {@code .jar} name. */
        private final String fileName;

        Fetched(Path temp, String moduleId, String version, String fileName) {
            this.temp = temp;
            this.moduleId = moduleId;
            this.version = version;
            this.fileName = fileName;
        }

        /** Deletes the temporary file, if it is still there. */
        public void discard() {
            deleteQuietly(temp);
        }
    }

    /**
     * Looks a module up by name or Plugin-Id, ignoring case.
     *
     * @return the module's registry entry; fails with a {@link RegistryException} when the
     *         registry does not know it or cannot be reached
     */
    public CompletableFuture<ModuleInfo> info(String name) {
        return CompletableFuture.supplyAsync(() -> infoNow(name));
    }

    /** {@link #info} on the calling thread. */
    public ModuleInfo infoNow(String name) {
        return new ModuleInfo(getJson("/modules/" + encode(name)));
    }

    /**
     * Every module the registry lists, on the calling thread.
     *
     * @param platform a platform tag id the latest version must run on, or {@code null} for all
     */
    public List<ModuleInfo> listNow(String platform) {
        String query = platform == null || platform.isBlank() ? "" : "?platform=" + encode(platform);
        JsonObject json = getJson("/modules" + query);
        List<ModuleInfo> modules = new ArrayList<>();
        if (json.has("modules") && json.get("modules").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("modules")) {
                if (! element.isJsonObject()) continue;
                ModuleInfo info = new ModuleInfo(element.getAsJsonObject());
                // The registry filters too; this keeps an older registry that ignores the
                // parameter from offering modules for another platform.
                if (platform == null || platform.isBlank() || info.runsOn(platform)) modules.add(info);
            }
        }
        return modules;
    }

    /** {@link #listNow} off the calling thread. */
    public CompletableFuture<List<ModuleInfo>> list(String platform) {
        return CompletableFuture.supplyAsync(() -> listNow(platform));
    }

    private JsonObject getJson(String path) {
        try {
            HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(apiUrl() + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RegistryException(errorMessage(response.statusCode(), response.body()));
            }
            return new JsonParser().parse(response.body()).getAsJsonObject();
        } catch (IOException e) {
            throw new RegistryException("Could not reach " + baseUrl() + ": " + e.getMessage(), e);
        } catch (IllegalStateException | com.google.gson.JsonParseException e) {
            throw new RegistryException("The module registry sent an unreadable answer.", e);
        }
    }

    /**
     * Downloads a module's jar into a temporary file in {@code folder}, on the calling thread,
     * and checks it against the registry's SHA-256. A transfer that fails or does not match
     * leaves nothing behind.
     *
     * @param name    the module name or Plugin-Id, matched case-insensitively by the registry
     * @param version a specific version, or {@code null} for the latest
     * @param folder  where the temporary file goes; the module folder, so the final move is a rename
     */
    public Fetched fetch(String name, String version, Path folder) {
        // "latest" is a registry keyword, never a real version.
        String target = version == null || version.isBlank() ? "latest" : version.trim();
        String url = apiUrl() + "/" + encode(name) + "/download/" + encode(target);

        Path temp;
        try {
            Files.createDirectories(folder);
            // Not a .jar, so a module scanner never picks up a partial download.
            temp = Files.createTempFile(folder, ".ecloud-", ".part");
        } catch (IOException e) {
            throw new RegistryException("Could not write to the module folder: " + e.getMessage(), e);
        }

        boolean keep = false;
        try {
            HttpResponse<Path> response = send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(2))
                    .header("Accept", "application/java-archive")
                    .GET().build(), HttpResponse.BodyHandlers.ofFile(temp));

            if (response.statusCode() != 200) {
                throw new RegistryException(errorMessage(response.statusCode(), Files.readString(temp)));
            }

            String moduleId = response.headers().firstValue("X-Module-Id").orElse(name);
            String moduleVersion = response.headers().firstValue("X-Module-Version").orElse(target);
            String expected = response.headers().firstValue("X-Checksum-SHA256").orElse(null);
            if (expected != null && ! expected.equalsIgnoreCase(sha256(temp))) {
                throw new RegistryException("Checksum mismatch for '" + moduleId + "'; the download was discarded.");
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
            throw new RegistryException("Could not reach " + baseUrl() + ": " + e.getMessage(), e);
        } finally {
            if (! keep) deleteQuietly(temp);
        }
    }

    /**
     * Compares dotted versions part by part, numerically where both parts are numbers
     * ({@code 1.10.0} is newer than {@code 1.9.2}); anything after a {@code -} or {@code +}
     * is ignored.
     */
    public static int compareVersions(String a, String b) {
        String[] left = a.trim().split("[-+]", 2)[0].split("\\.");
        String[] right = b.trim().split("[-+]", 2)[0].split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            String l = i < left.length ? left[i] : "0";
            String r = i < right.length ? right[i] : "0";
            int compared;
            try {
                compared = Long.compare(Long.parseLong(l), Long.parseLong(r));
            } catch (NumberFormatException e) {
                compared = l.compareToIgnoreCase(r);
            }
            if (compared != 0) return compared;
        }
        return 0;
    }

    /** A main attribute of a jar's manifest, trimmed, or {@code null}. */
    public static String manifestValue(Path jar, String attribute) {
        try (JarFile file = new JarFile(jar.toFile())) {
            Manifest manifest = file.getManifest();
            if (manifest == null) return null;
            String value = manifest.getMainAttributes().getValue(attribute);
            return value == null ? null : value.trim();
        } catch (IOException e) {
            return null;
        }
    }

    /** The platform tag ids in a jar's {@code Plugin-Platforms}, lower case; empty if it has none. */
    public static List<String> manifestPlatforms(Path jar) {
        String raw = manifestValue(jar, "Plugin-Platforms");
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        List<String> tags = new ArrayList<>();
        for (String part : raw.split(",")) {
            String tag = part.trim().toLowerCase(Locale.ROOT);
            if (! tag.isEmpty()) tags.add(tag);
        }
        return tags;
    }

    /** Moves a file into place, atomically where the file system allows it. */
    public static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A leftover .part file is harmless; it is never loaded.
        }
    }

    public static String sha256(Path file) throws IOException {
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

    private static <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            return CLIENT.send(request, handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted", e);
        }
    }

    /** The configured registry root, without a trailing slash. */
    public String baseUrl() {
        String url = baseUrl.get();
        url = url == null || url.isBlank() ? DEFAULT_BASE_URL : url.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String apiUrl() {
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

    private static String string(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private static long number(JsonObject json, String key) {
        JsonElement element = json.get(key);
        try {
            return element == null || element.isJsonNull() ? 0L : element.getAsLong();
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    private static List<String> strings(JsonObject json, String key) {
        List<String> values = new ArrayList<>();
        JsonElement element = json.get(key);
        if (element == null || ! element.isJsonArray()) return values;
        for (JsonElement value : element.getAsJsonArray()) {
            if (value.isJsonPrimitive()) values.add(value.getAsString());
        }
        return values;
    }

    private static boolean containsIgnoreCase(List<String> values, String wanted) {
        for (String value : values) {
            if (value.equalsIgnoreCase(wanted)) return true;
        }
        return false;
    }
}
