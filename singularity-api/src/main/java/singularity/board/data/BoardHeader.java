package singularity.board.data;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the category header of a {@link BoardData} entry.
 *
 * <p>A header is a string label that categorises board data. It can represent
 * a plain string, the simple name of a Java class (suffixed with {@code .class}),
 * or the sentinel value {@link #EMPTY} when no header has been set.
 */
@Setter
@Getter
public class BoardHeader {

    /**
     * Sentinel value used to indicate an absent or unset header.
     */
    public static final String EMPTY = ">>EMPTY<<";

    /** The raw header string value. */
    private String header;

    /**
     * Constructs a {@code BoardHeader} with the given raw header string.
     *
     * @param header the header value; must not be {@code null}
     */
    public BoardHeader(String header) {
        this.header = header;
    }

    /**
     * Constructs a {@code BoardHeader} derived from the simple name of a class.
     *
     * <p>The {@code .class} suffix is appended if it is not already present.
     *
     * @param clazz the class whose simple name is used as the header
     */
    public BoardHeader(Class<?> clazz) {
        this.header = clazz.getSimpleName();
        if (! this.header.endsWith(".class"))
            this.header = this.header.concat(".class");
    }

    /**
     * Constructs an empty {@code BoardHeader} using the {@link #EMPTY} sentinel.
     */
    public BoardHeader() {
        this.header = EMPTY;
    }

    /**
     * Returns {@code true} if this header holds the {@link #EMPTY} sentinel value.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return this.header.equals(EMPTY);
    }

    /**
     * Returns {@code true} if this header ends with {@code .class}, indicating
     * it was derived from a class name.
     *
     * @return {@code true} if the header represents a class-based category
     */
    public boolean isOfAnyClass() {
        return this.header.endsWith(".class");
    }

    /**
     * Returns {@code true} if this header matches the simple name of the
     * given class (without the {@code .class} suffix).
     *
     * @param clazz the class to compare against
     * @return {@code true} if the header equals the class's simple name
     */
    public boolean isOfClass(Class<?> clazz) {
        return this.header.equals(clazz.getSimpleName());
    }
}
