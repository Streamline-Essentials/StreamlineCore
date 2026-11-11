package gg.drak.thebase.utils;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class StringUtils {
    public static String color(String s) {
        return s.replace("&", "§");
    }

    public static String uncolor(String s) {
        return s.replace("§", "&");
    }

    public static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static String uncapitalize(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    public static String capitalizeAll(String s) {
        String[] split = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String s1 : split) {
            sb.append(capitalize(s1)).append(" ");
        }
        return sb.toString().trim();
    }

    public static String uncapitalizeAll(String s) {
        String[] split = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String s1 : split) {
            sb.append(uncapitalize(s1)).append(" ");
        }
        return sb.toString().trim();
    }

    public static <T> List<String> stringify(List<T> list) {
        List<String> strings = new ArrayList<>();
        for (T t : list) {
            strings.add(t.toString());
        }
        return strings;
    }

    public static <T> T[] newEmptyArray() {
        return (T[]) new Object[0];
    }

    public static <T> T[] argsMinus(T[] array, int... indexes) {
        List<Integer> indexesList = new ArrayList<>();
        for (int index : indexes) {
            indexesList.add(index);
        }
        List<T> list = new ArrayList<>();
        for (int i = 0; i < array.length; i ++) {
            if (! indexesList.contains(i)) {
                list.add(array[i]);
            }
        }
        return list.toArray(newEmptyArray());
    }

    public static String[] argsMinus(String[] array, int... indexes) {
        List<Integer> indexesList = new ArrayList<>();
        for (int index : indexes) {
            indexesList.add(index);
        }
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length; i ++) {
            if (! indexesList.contains(i)) {
                list.add(array[i]);
            }
        }
        return list.toArray(new String[0]);
    }

    public static <T> String argsToString(T[] args) {
        StringBuilder sb = new StringBuilder();
        for (T arg : args) {
            sb.append(arg).append(" ");
        }
        return sb.toString().trim();
    }

    public static String argsToString(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg).append(" ");
        }
        return sb.toString().trim();
    }

    public static <T> String argsToStringMinus(T[] args, int... indexes) {
        T[] argsMinus = argsMinus(args, indexes);
        return argsToString(argsMinus);
    }

    public static String argsToStringMinus(String[] args, int... indexes) {
        String[] argsMinus = argsMinus(args, indexes);
        return argsToString(argsMinus);
    }

    public static <T> T[] argsWithoutNulls(T[] args) {
        List<T> list = new ArrayList<>();
        for (T arg : args) {
            if (arg != null) {
                list.add(arg);
            }
        }
        return list.toArray(args);
    }

    public static String[] argsWithoutEmptyOrNull(String[] args) {
        List<String> list = new ArrayList<>();

        for (String arg : args) {
            if (arg != null && ! arg.isEmpty()) {
                list.add(arg);
            }
        }

        return list.toArray(args);
    }

    public static String[] argsWithoutEmptyOrNullMinus(String[] args, int... indexes) {
        String[] argsMinus = argsMinus(args, indexes);
        return argsWithoutEmptyOrNull(argsMinus);
    }

    public static String removeNonIdentifierSafeChars(String string) {
        return string.replaceAll("[^a-zA-Z0-9_\\-+]", "");
    }

    public static ConcurrentSkipListSet<String> getAsCompletion(String toComplete, String... listOfPossibilities) {
        ConcurrentSkipListSet<String> strings = new ConcurrentSkipListSet<>();

        if (toComplete == null) return new ConcurrentSkipListSet<>(List.of(listOfPossibilities));
        if (toComplete.isEmpty() || toComplete.isBlank()) return new ConcurrentSkipListSet<>(List.of(listOfPossibilities));

        for (String possibility : listOfPossibilities) {
            if (possibility.toLowerCase().startsWith(toComplete.toLowerCase())) {
                strings.add(possibility);
            }
        }

        return strings;
    }

    public static ConcurrentSkipListSet<String> getAsCompletion(String toComplete, Collection<String> listOfPossibilities) {
        return getAsCompletion(toComplete, listOfPossibilities.toArray(new String[0]));
    }

    public static List<String> getAsCompletionList(String toComplete, String... listOfPossibilities) {
        return new ArrayList<>(getAsCompletion(toComplete, listOfPossibilities));
    }

    public static List<String> getAsCompletionList(String toComplete, Collection<String> listOfPossibilities) {
        return new ArrayList<>(getAsCompletion(toComplete, listOfPossibilities));
    }

    public static ConcurrentSkipListSet<String> getAsCompletion(String[] arguments, String... listOfPossibilities) {
        ConcurrentSkipListSet<String> strings = new ConcurrentSkipListSet<>();

        if (arguments == null) return new ConcurrentSkipListSet<>(List.of(listOfPossibilities));
        if (arguments.length == 0) return new ConcurrentSkipListSet<>(List.of(listOfPossibilities));

        return getAsCompletion(arguments[arguments.length - 1], listOfPossibilities);
    }

    public static ConcurrentSkipListSet<String> getAsCompletion(String[] arguments, Collection<String> listOfPossibilities) {
        return getAsCompletion(arguments, listOfPossibilities.toArray(new String[0]));
    }

    public static List<String> getAsCompletionList(String[] arguments, String... listOfPossibilities) {
        return new ArrayList<>(getAsCompletion(arguments, listOfPossibilities));
    }

    public static List<String> getAsCompletionList(String[] arguments, Collection<String> listOfPossibilities) {
        return new ArrayList<>(getAsCompletion(arguments, listOfPossibilities));
    }

    public static List<String> stringToList(String string, String separator) {
        List<String> list = new ArrayList<>();

        if (string == null) return list;
        if (string.isEmpty()) return list;

        String[] split = string.split(separator);
        list.addAll(Arrays.asList(split));

        return list;
    }

    public static List<String> stringToList(String string) {
        return stringToList(string, ",");
    }

    public static String listToString(List<String> list, String separator) {
        StringBuilder sb = new StringBuilder();

        for (String string : list) {
            sb.append(string).append(separator);
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString().trim();
    }

    public static String listToString(List<String> list) {
        return listToString(list, ",");
    }
}
