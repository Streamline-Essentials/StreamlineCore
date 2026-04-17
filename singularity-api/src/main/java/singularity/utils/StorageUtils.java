package singularity.utils;

import singularity.Singularity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Utility class for reading classpath-based resource files.
 *
 * <p>Methods in this class load resources through {@link Singularity}'s class loader so
 * that resources bundled inside the Singularity JAR are always accessible regardless of
 * the active platform.
 */
public class StorageUtils {

    /**
     * Reads the {@code singularity.properties} resource file from the classpath and
     * returns its key/value pairs as a sorted map.
     *
     * <p>Each line is expected to follow the format {@code key=value}; lines that do
     * not contain exactly one {@code =} are silently skipped.
     *
     * @return a sorted map of property keys to their string values; empty if the file
     *         cannot be found or read
     */
    public static ConcurrentSkipListMap<String, String> readProperties() {
        ConcurrentSkipListMap<String, String> map = new ConcurrentSkipListMap<>();

        List<String> lines = getLinesFromResourceFile("singularity.properties");

        for (String line : lines) {
            String[] parts = line.split("=", 2);
            if (parts.length != 2) continue;
            String key = parts[0];
            String value = parts[1];
            map.put(key, value);
        }

        return map;
    }

    /**
     * Reads all lines from a named resource file on the classpath.
     *
     * @param name the classpath-relative name of the resource (e.g.,
     *             {@code "singularity.properties"})
     * @return a list of lines in the order they appear in the file; empty if the
     *         resource is not found or an I/O error occurs
     */
    public static List<String> getLinesFromResourceFile(String name) {
        List<String> lines = new ArrayList<>();
        try {
            InputStream is = Singularity.class.getClassLoader().getResourceAsStream(name);
            if (is == null) return lines;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lines;
    }
}
