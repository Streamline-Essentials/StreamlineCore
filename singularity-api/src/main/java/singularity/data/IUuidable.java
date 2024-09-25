package singularity.data;

import tv.quaint.objects.Identifiable;

import java.util.UUID;

public interface IUuidable extends Identifiable {
    String getUuid();
    void setUuid(String uuid);

    @Override
    default String getIdentifier() {
        return getUuid();
    }

    @Override
    default void setIdentifier(String uuid) {
        setUuid(uuid);
    }

    default void setUuid(UUID uuid) {
        setUuid(uuid.toString());
    }

    default UUID getRealUuid() {
        return UUID.fromString(getUuid());
    }

    default boolean hasValidUuid() {
        try {
            getRealUuid();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    default boolean isUuidContainsDashes() {
        return getUuid().contains("-");
    }
}
