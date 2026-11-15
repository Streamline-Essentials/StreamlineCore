package gg.drak.thebase.numbers;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Random;

@Setter
@Getter
public class WeightedInteger extends Number implements Comparable<WeightedInteger> {
    private Random RNG;
    private WeightedInteger min;
    private WeightedInteger max;
    private Date separator;
    private int absoluteMin;
    private int absoluteMax;

    public WeightedInteger(int min, int max, Random RNG) {
        setSeparator(new Date());
        setAbsoluteMin(min);
        setAbsoluteMax(max);
        setRNG(RNG);
    }

    public WeightedInteger(int min, int max, long seed) {
        this(min, max, new Random(seed));
    }

    public WeightedInteger(int min, int max) {
        this(min, max, new Random());
    }

    public WeightedInteger(WeightedInteger min, WeightedInteger max, Random RNG) {
        setSeparator(new Date());
        setMin(min);
        setMax(max);
        setRNG(RNG);
    }

    public WeightedInteger(WeightedInteger min, WeightedInteger max, long seed) {
        this(min, max, new Random(seed));
    }

    public WeightedInteger(WeightedInteger min, WeightedInteger max) {
        this(min, max, new Random());
    }

    public WeightedInteger(int min, WeightedInteger max, Random RNG) {
        setSeparator(new Date());
        setAbsoluteMin(min);
        setMax(max);
        setRNG(RNG);
    }

    public WeightedInteger(int min, WeightedInteger max, long seed) {
        this(min, max, new Random(seed));
    }

    public WeightedInteger(int min, WeightedInteger max) {
        this(min, max, new Random());
    }

    public WeightedInteger(WeightedInteger min, int max, Random RNG) {
        setSeparator(new Date());
        setMin(min);
        setAbsoluteMax(max);
        setRNG(RNG);
    }

    public WeightedInteger(WeightedInteger min, int max, long seed) {
        this(min, max, new Random(seed));
    }

    public WeightedInteger(WeightedInteger min, int max) {
        this(min, max, new Random());
    }

    public int roll() {
        if (getMin() != null) setAbsoluteMin(getMin().roll());
        if (getMax() != null) setAbsoluteMax(getMax().roll());
        if (getAbsoluteMin() >= getAbsoluteMax()) return getAbsoluteMin();
        return getRNG().nextInt(getAbsoluteMax() + 1 - getAbsoluteMin()) + getAbsoluteMin();
    }

    @Override
    public int compareTo(@NotNull WeightedInteger o) {
        return Long.compare(getSeparator().getTime(), o.getSeparator().getTime());
    }

    @Override
    public int intValue() {
        return roll();
    }

    @Override
    public long longValue() {
        return roll();
    }

    @Override
    public float floatValue() {
        return roll();
    }

    @Override
    public double doubleValue() {
        return roll();
    }
}
