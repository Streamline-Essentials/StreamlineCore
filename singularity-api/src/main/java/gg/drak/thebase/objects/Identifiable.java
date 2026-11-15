package gg.drak.thebase.objects;

/**
 * An interface for objects that have an identifier.
 */
public interface Identifiable extends Identified {
    /**
     * Set the identifier of this object.
     * @param identifier The identifier to set.
     */
    public void setIdentifier(String identifier);
}
