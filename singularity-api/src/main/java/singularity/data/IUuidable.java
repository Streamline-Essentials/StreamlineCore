package singularity.data;

import gg.drak.thebase.objects.Identifiable;

import java.util.UUID;

/**
 * Contract for any object that can be identified by a UUID string.
 * Extends {@link Identifiable} and bridges the string-based UUID to
 * the {@link java.util.UUID} type.
 */
public interface IUuidable extends Identifiable {

    /**
     * Returns the UUID of this object as a plain string.
     *
     * @return the string-form UUID
     */
    String getUuid();

    /**
     * Sets the UUID of this object from a plain string.
     *
     * @param uuid the string-form UUID to assign
     */
    void setUuid(String uuid);

    /**
     * {@inheritDoc}
     */
    @Override
    default String getIdentifier() {
        return getUuid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default void setIdentifier(String uuid) {
        setUuid(uuid);
    }

    /**
     * Sets the UUID of this object from a {@link UUID} instance, converting it
     * to its canonical string representation.
     *
     * @param uuid the {@link UUID} to assign
     */
    default void setUuid(UUID uuid) {
        setUuid(uuid.toString());
    }

    /**
     * Parses and returns the UUID as a {@link UUID} object.
     *
     * @return a {@link UUID} instance derived from this object's string UUID
     * @throws IllegalArgumentException if the stored string is not a valid UUID
     */
    default UUID getRealUuid() {
        return UUID.fromString(getUuid());
    }

    /**
     * Returns whether this object currently holds a syntactically valid UUID string.
     *
     * @return {@code true} if the UUID string can be parsed; {@code false} otherwise
     */
    default boolean hasValidUuid() {
        try {
            getRealUuid();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns whether the UUID string contains dashes (i.e. is in the standard
     * {@code xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx} format).
     *
     * @return {@code true} if the UUID string contains at least one dash
     */
    default boolean isUuidContainsDashes() {
        return getUuid().contains("-");
    }
}
