package gg.drak.thebase.objects;

import org.jetbrains.annotations.NotNull;

/**
 * An interface for objects that have an index.
 */
public interface Indexed extends Comparable<Indexed> {
    /**
     * Get the identifier of this object.
     * @return The identifier of this object.
     */
    public int getIndex();

    /**
     * Compare two {@link Indexed} objects.
     * @param o the {@link Indexed} object to compare to.
     * @return the result of the comparison.
     */
    @Override
    default int compareTo(@NotNull Indexed o) {
        return Integer.compare(getIndex(), o.getIndex());
    }
}
