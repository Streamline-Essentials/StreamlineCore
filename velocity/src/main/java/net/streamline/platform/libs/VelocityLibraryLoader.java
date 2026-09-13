package net.streamline.platform.libs;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Downloads Maven Central libraries declared in {@code velocity-libraries.txt} and
 * attaches them to this plugin's classpath via {@link PluginManager#addToClasspath}.
 *
 * <p>Velocity has no native {@code libraries:} downloader like Spigot/Bungee, so this
 * runs at the start of proxy init before Singularity touches Hikari/Lettuce/etc.</p>
 */
public final class VelocityLibraryLoader {
    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";
    private static final String RESOURCE = "/velocity-libraries.txt";

    private VelocityLibraryLoader() {
    }

    /**
     * Ensures every GAV listed in {@code velocity-libraries.txt} is present on the
     * plugin classpath.
     *
     * @param server    the proxy server
     * @param container this plugin's container
     * @param dataDir   plugin data directory (libraries are cached under {@code libraries/})
     * @param logger    plugin logger
     */
    public static void ensureLoaded(ProxyServer server, PluginContainer container, Path dataDir, Logger logger) {
        List<String> coordinates = readCoordinates();
        if (coordinates.isEmpty()) {
            logger.warn("No entries found in {}; skipping runtime library load.", RESOURCE);
            return;
        }

        Path libDir = dataDir.resolve("libraries");
        try {
            Files.createDirectories(libDir);
        } catch (Exception e) {
            logger.error("Failed to create libraries directory: {}", libDir, e);
            return;
        }

        PluginManager pluginManager = server.getPluginManager();
        ClassLoader classLoader = VelocityLibraryLoader.class.getClassLoader();
        int loaded = 0;
        int skipped = 0;

        for (String coord : coordinates) {
            Artifact artifact = Artifact.parse(coord);
            if (artifact == null) {
                logger.warn("Skipping invalid library coordinate: {}", coord);
                continue;
            }

            if (isPresent(artifact.testClassHint(), classLoader)) {
                skipped++;
                continue;
            }

            try {
                Path jar = resolveJar(libDir, artifact);
                pluginManager.addToClasspath(container, jar);
                loaded++;
                logger.info("Loaded runtime library: {}", artifact.coords());
            } catch (Throwable t) {
                logger.error("Failed to load runtime library {}: {}", artifact.coords(), t.getMessage(), t);
            }
        }

        if (loaded > 0) {
            logger.info("Velocity library loader attached {} jar(s) ({} already present).", loaded, skipped);
        }
    }

    private static List<String> readCoordinates() {
        InputStream in = VelocityLibraryLoader.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            return Collections.emptyList();
        }

        List<String> out = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.contains("${")) {
                    continue;
                }
                out.add(line);
            }
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
        return out;
    }

    private static Path resolveJar(Path libDir, Artifact artifact) throws Exception {
        Path target = libDir.resolve(artifact.fileName());
        if (Files.exists(target) && Files.size(target) > 0L) {
            return target;
        }

        URL url = new URL(MAVEN_CENTRAL + artifact.mavenPath());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(60_000);
        connection.setRequestProperty("User-Agent", "StreamlineCore-VelocityLibraryLoader");

        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code + " for " + url);
        }

        Path temp = Files.createTempFile(libDir, artifact.artifact + "-", ".jar.part");
        try (InputStream stream = connection.getInputStream()) {
            Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
        }

        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private static boolean isPresent(String className, ClassLoader classLoader) {
        if (className == null) {
            return false;
        }
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class Artifact {
        private final String group;
        private final String artifact;
        private final String version;

        private Artifact(String group, String artifact, String version) {
            this.group = group;
            this.artifact = artifact;
            this.version = version;
        }

        static Artifact parse(String coord) {
            String[] parts = coord.split(":");
            if (parts.length < 3) {
                return null;
            }
            return new Artifact(parts[0], parts[1], parts[2]);
        }

        String coords() {
            return group + ":" + artifact + ":" + version;
        }

        String fileName() {
            return artifact + "-" + version + ".jar";
        }

        String mavenPath() {
            return group.replace('.', '/') + "/" + artifact + "/" + version + "/" + fileName();
        }

        /**
         * Best-effort "already loaded" probe for a few well-known artifacts.
         * Unknown artifacts are always downloaded/attached.
         */
        String testClassHint() {
            switch (artifact) {
                case "HikariCP":
                    return "com.zaxxer.hikari.HikariDataSource";
                case "slf4j-api":
                    return "org.slf4j.Logger";
                case "sqlite-jdbc":
                    return "org.sqlite.JDBC";
                case "mysql-connector-j":
                    return "com.mysql.cj.jdbc.Driver";
                case "gson":
                    return "com.google.gson.Gson";
                case "caffeine":
                    return "com.github.benmanes.caffeine.cache.Cache";
                case "pf4j":
                    return "org.pf4j.PluginManager";
                case "logback-classic":
                    return "ch.qos.logback.classic.Logger";
                case "guava":
                    return "com.google.common.collect.ImmutableList";
                case "lettuce-core":
                    return "io.lettuce.core.RedisClient";
                case "netty-common":
                    return "io.netty.util.concurrent.Future";
                case "reactor-core":
                    return "reactor.core.publisher.Mono";
                default:
                    return null;
            }
        }
    }
}
