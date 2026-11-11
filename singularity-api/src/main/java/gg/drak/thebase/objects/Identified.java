package gg.drak.thebase.objects;

import org.jetbrains.annotations.NotNull;

/**
 * An interface for objects that have an identifier.
 */
public interface Identified extends Comparable<Identified> {
    /**
     * Get the identifier of this object.
     * @return The identifier of this object.
     */
    public String getIdentifier();

    /**
     * Compare two {@link Identified} objects.
     * @param o the {@link Identified} object to compare to.
     * @return the result of the comparison.
     */
    @Override
    default int compareTo(@NotNull Identified o) {
        return getIdentifier().compareTo(o.getIdentifier());
    }
}
