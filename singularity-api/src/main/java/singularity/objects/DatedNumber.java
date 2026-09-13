package singularity.objects;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * A {@link Number} wrapper that records the {@link Date} at which the value
 * was created, allowing numeric values to be associated with a point in time.
 *
 * <p>{@code DatedNumber} extends {@link Number} so it can be used wherever a
 * standard numeric type is accepted, and implements {@link Comparable} so that
 * instances can be sorted by their numeric value (not by timestamp).
 *
 * @param <T> the concrete {@link Number} subtype being wrapped (e.g.
 *            {@link Integer}, {@link Double})
 */
@Getter
public class DatedNumber<T extends Number> extends Number implements Comparable<T> {

    /**
     * The wrapped numeric value.  Immutable after construction.
     */
    private final T number;

    /**
     * The timestamp recording when this {@code DatedNumber} was created.
     * Immutable after construction.
     */
    private final Date date;

    /**
     * Constructs a {@code DatedNumber} with an explicit timestamp.
     *
     * @param number the numeric value to wrap; must not be {@code null}
     * @param date   the timestamp to associate with the value; must not be
     *               {@code null}
     */
    public DatedNumber(T number, Date date) {
        this.number = number;
        this.date = date;
    }

    /**
     * Constructs a {@code DatedNumber} whose timestamp is set to the current
     * wall-clock time at the moment of construction.
     *
     * @param number the numeric value to wrap; must not be {@code null}
     */
    public DatedNumber(T number) {
        this(number, new Date());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Comparison is performed against the unwrapped value of the given
     * number using {@link Double#compare(double, double)} to ensure consistent
     * ordering across all {@link Number} subtypes.
     */
    @Override
    public int compareTo(@NotNull T otherValue) {
        return compare(getNumber(), otherValue);
    }

    /**
     * Compares two {@link Number} values by converting both to {@code double}
     * and delegating to {@link Double#compare(double, double)}.
     *
     * @param <T> the {@link Number} subtype
     * @param x   the first value
     * @param y   the second value
     * @return a negative integer, zero, or a positive integer as {@code x} is
     *         less than, equal to, or greater than {@code y}
     */
    public static <T extends Number> int compare(T x, T y) {
        return Double.compare(x.doubleValue(), y.doubleValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int intValue() {
        return getNumber().intValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long longValue() {
        return getNumber().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float floatValue() {
        return getNumber().floatValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double doubleValue() {
        return getNumber().doubleValue();
    }
}
