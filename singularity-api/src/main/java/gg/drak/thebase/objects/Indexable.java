package gg.drak.thebase.objects;

/**
 * An interface for objects that have an index.
 */
public interface Indexable extends Indexed {
    /**
     * Set the index of this object.
     * @param index The index to set.
     */
    public void setIndex(int index);
}
