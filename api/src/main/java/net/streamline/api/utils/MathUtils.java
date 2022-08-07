package net.streamline.api.utils;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

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
}
