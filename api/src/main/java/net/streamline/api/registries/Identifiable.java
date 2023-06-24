package net.streamline.api.registries;

import lombok.Getter;

public interface Identifiable extends Comparable<Identifiable> {
    public String getIdentifier();

    public void setIdentifier(String identifier);

    @Override
    public default int compareTo(Identifiable other) {
        return getIdentifier().compareTo(other.getIdentifier());
    }
}
