package singularity.permissions;

import gg.drak.thebase.objects.Identified;
import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.utils.UserUtils;

import java.util.Optional;

/**
 * Represents a single permission meta-data entry (e.g. a prefix or suffix)
 * that is associated with a specific player.
 *
 * <p>The {@link #identifier} field holds the player's unique identifier so that
 * the owning {@link CosmicPlayer} can be resolved on demand via {@link #getOn()}.
 */
@Getter @Setter
public class MetaValue implements Identified {

    /**
     * The unique identifier of the player this meta-value belongs to.
     * Used to look up the {@link CosmicPlayer} via {@link #getOn()}.
     */
    private String identifier;

    /** The category of this meta-data (e.g. {@link MetaKey#PREFIX} or {@link MetaKey#SUFFIX}). */
    private MetaKey key;

    /** The raw text content of this meta-data entry (e.g. the actual prefix string). */
    private String value;

    /**
     * The UNIX timestamp (in milliseconds) at which this meta-value expires,
     * or {@code 0} / a negative value to indicate no expiration.
     */
    private long expiration;

    /**
     * The priority of this meta-data entry relative to other entries of the
     * same key. Higher values take precedence.
     */
    private int priority;

    /**
     * Constructs a new {@code MetaValue}.
     *
     * @param identifier the unique identifier of the owning player
     * @param key        the category of meta-data ({@link MetaKey#PREFIX} or {@link MetaKey#SUFFIX})
     * @param value      the text content of the meta entry
     * @param expiration the expiration timestamp in milliseconds, or {@code 0} for permanent
     * @param priority   the priority of this entry; higher values win when multiple entries exist
     */
    public MetaValue(String identifier, MetaKey key, String value, long expiration, int priority) {
        this.identifier = identifier;
        this.key = key;
        this.value = value;
        this.expiration = expiration;
        this.priority = priority;
    }

    /**
     * Resolves the {@link CosmicPlayer} that owns this meta-value.
     *
     * <p>The player is looked up (and created if absent) using
     * {@link UserUtils#getOrCreatePlayer(String)} with this entry's
     * {@link #identifier}.
     *
     * @return an {@link Optional} containing the owning {@link CosmicPlayer},
     *         or {@link Optional#empty()} if the player cannot be resolved
     */
    public Optional<CosmicPlayer> getOn() {
        return UserUtils.getOrCreatePlayer(identifier);
    }
}
