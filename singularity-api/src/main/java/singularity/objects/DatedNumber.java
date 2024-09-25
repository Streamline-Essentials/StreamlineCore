package singularity.objects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

@Getter
public class DatedNumber<T extends Number> extends Number implements Comparable<T> {
    private final T number;
    private final Date date;

    public DatedNumber(T number, Date date) {
        this.number = number;
        this.date = date;
    }

    public DatedNumber(T number) {
        this(number, new Date());
    }

    @Override
    public int compareTo(@NotNull T otherValue) {
        return compare(getNumber(), otherValue);
    }

    public static <T extends Number> int compare(T x, T y) {
        return Double.compare(x.doubleValue(), y.doubleValue());
    }

    @Override
    public int intValue() {
        return getNumber().intValue();
    }

    @Override
    public long longValue() {
        return getNumber().longValue();
    }

    @Override
    public float floatValue() {
        return getNumber().floatValue();
    }

    @Override
    public double doubleValue() {
        return getNumber().doubleValue();
    }
}
