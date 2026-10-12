package singularity.events.entity;

import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.events.components.BaseEvent;
import gg.drak.thebase.events.components.FunctionedCall;
import lombok.Getter;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.events.CosmicEvent;

import java.util.Set;

/**
 * Base for events about a non-player entity changing the world, fired on backend servers
 * <em>before</em> the change happens so listeners can prevent it.
 *
 * <p>TheBase delivers an event only to listeners of its exact class, so listen to the
 * concrete subclasses, not to this one.</p>
 *
 * <p>Entity ids are lowercase namespaced ids, such as {@code minecraft:creeper}, on every
 * platform. Worlds are named as in {@link PlayerWorld}: the world name on Spigot, the
 * dimension id on the mod loaders.</p>
 */
@Getter
public abstract class CosmicEntityEvent extends CosmicEvent {
    private final PlayerWorld world;
    private final WorldPosition position;

    protected CosmicEntityEvent(PlayerWorld world, WorldPosition position) {
        this.world = world;
        this.position = position;
    }

    /**
     * Whether anything would receive an event of this exact class. Platforms fire some of
     * these from per-tick hooks, and check this first so servers where no module listens
     * pay nothing for them.
     */
    public static boolean hasListeners(Class<? extends BaseEvent> type) {
        Set<?> listeners = BaseEventHandler.getRegularEvents().get(type);
        if (listeners != null && ! listeners.isEmpty()) return true;

        for (FunctionedCall<BaseEvent> function : BaseEventHandler.getFunctions()) {
            if (function.getClazz().isAssignableFrom(type)) return true;
        }
        return false;
    }
}
