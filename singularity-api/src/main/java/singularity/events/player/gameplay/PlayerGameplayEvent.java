package singularity.events.player.gameplay;

import lombok.Getter;
import singularity.data.players.CosmicPlayer;
import singularity.events.CosmicEvent;

import java.util.Locale;

/**
 * Base for events reporting something a player did in the world, fired once the action is final:
 * after every plugin or mod had the chance to cancel it. Listeners use these to count or reward
 * what happened, not to prevent it.
 *
 * <p>Ids of blocks, items and entities are lowercase namespaced ids, such as
 * {@code minecraft:wheat}, on every platform.</p>
 */
@Getter
public abstract class PlayerGameplayEvent extends CosmicEvent {
    private final CosmicPlayer player;
    /** The player's game mode, lowercase: {@code survival}, {@code creative}, {@code adventure} or {@code spectator}. */
    private final String gameMode;

    protected PlayerGameplayEvent(CosmicPlayer player, String gameMode) {
        this.player = player;
        this.gameMode = gameMode == null ? "survival" : gameMode.toLowerCase(Locale.ROOT);
    }

    /** Whether the player is in creative or spectator mode. */
    public boolean isCreativeLike() {
        return gameMode.equals("creative") || gameMode.equals("spectator");
    }
}
