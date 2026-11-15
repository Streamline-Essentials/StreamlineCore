package gg.drak.thebase.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ObjectUtils {
    public static <K, V> Map<K, V> interpolateMap(Map<K, V> map) {
        if (map.isEmpty()) {
            return map;
        }

        List<K> keys = new ArrayList<>(map.keySet());
        List<V> values = new ArrayList<>(map.values());

        Random RNG = new Random();
        int keyShift = RNG.nextInt(keys.size() - 1);
        int valueShift = RNG.nextInt(values.size() - 1);

        List<K> finalKeys = new ArrayList<>();
        List<V> finalValues = new ArrayList<>();

        for (int i = 0; i < keys.size(); i ++) {
            finalKeys.add(keys.get((i + keyShift) % keys.size()));
            finalValues.add(values.get((i + valueShift) % values.size()));
        }

        map.clear();

        for (int i = 0; i < keys.size(); i ++) {
            map.put(finalKeys.get(i), finalValues.get(i));
        }

        return map;
    }
}
