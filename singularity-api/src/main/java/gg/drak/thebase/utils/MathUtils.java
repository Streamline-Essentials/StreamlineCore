package gg.drak.thebase.utils;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MathUtils {
    public static double eval(String function) {
        Expression expression = new ExpressionBuilder(function).build();
        return expression.evaluate();
    }

    public static long get30DMilli() {
        return 30L * 24L * 60L * 60L * 1000L;
    }

    public static long getCurrentMillis() {
        return new Date().toInstant().toEpochMilli();
    }

    public static int getInt(double value) {
        return ((Double) value).intValue();
    }

    public static boolean isDateOlderThan(Date check, int value, ChronoUnit temporalUnit) {
        return check.toInstant().isBefore(Instant.now().minus(value, temporalUnit));
    }

    public static int getCeilingInt(Set<Integer> ints){
        int value = 0;

        for (Integer i : ints) {
            if (i >= value) value = i;
        }

        return value;
    }

    public static <K, V> int remove(Map<K, V> collection, V object) {
        AtomicInteger amount = new AtomicInteger(0);
        Map<K, V> other = new HashMap<>(collection);

        other.forEach((o, t) -> {
            if (t.equals(object)) {
                collection.remove(o);
                amount.incrementAndGet();
            }
        });

        return amount.get();
    }

    public static <T> Map<Integer, T> mappifyArray(T[] array, int offset) {
        Map<Integer, T> r = new HashMap<>();

        for (int i = 0; i < array.length; i ++) {
            r.put(i + offset, array[i]);
        }

        return r;
    }

    public static <T> Map<Integer, T> mappifyArray(T[] array) {
        return mappifyArray(array, 0);
    }

    public static int getStringCount(String toCount, String on) {
        int count = 0;

        String temp = on;
        String regexMatch = MatcherUtils.makeLiteral(toCount);
        while (temp.contains(toCount)) {
            temp = temp.replaceFirst(regexMatch, "");
            count ++;
        }

        return count;
    }
}
