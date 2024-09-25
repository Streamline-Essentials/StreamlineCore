package singularity.utils;

import singularity.Singularity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

public class StorageUtils {
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
