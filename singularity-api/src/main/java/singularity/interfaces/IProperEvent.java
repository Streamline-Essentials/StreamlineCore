package singularity.interfaces;

import singularity.events.CosmicEvent;

public interface IProperEvent<E> {
    E getEvent();

    CosmicEvent getCosmicEvent();

    void setEvent(E event);

    void setCosmicEvent(CosmicEvent cosmicEvent);
}
