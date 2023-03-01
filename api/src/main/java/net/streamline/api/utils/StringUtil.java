package net.streamline.api.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class StringUtil {
    public static String concat(List<String> list, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            builder.append(list.get(i));
            if (i != list.size() - 1) builder.append(separator);
        }
        return builder.toString();
    }

    public static String concat(ConcurrentSkipListSet<String> list, String separator) {
        return concat(new ArrayList<>(list), separator);
    }

    public static String concat(String[] list, String separator) {
        return concat(Arrays.asList(list), separator);
    }

    public static List<String> split(String string, String separator) {
        return Arrays.asList(splitToArray(string, separator));
    }

    public static String[] splitToArray(String string, String separator) {
        return string.split(separator);
    }

    public static ConcurrentSkipListSet<String> splitToConcurrentSet(String string, String separator) {
        return new ConcurrentSkipListSet<>(split(string, separator));
    }
}
