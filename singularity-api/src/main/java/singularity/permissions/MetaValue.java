package singularity.permissions;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.utils.UserUtils;
import tv.quaint.objects.Identified;

@Getter @Setter
public class MetaValue implements Identified {
    private String identifier;

    private MetaKey key;
    private String value;
    private long expiration;
    private int priority;

    public MetaValue(String identifier, MetaKey key, String value, long expiration, int priority) {
        this.identifier = identifier;
        this.key = key;
        this.value = value;
        this.expiration = expiration;
        this.priority = priority;
    }

    public CosmicPlayer getOn() {
        return UserUtils.getOrCreatePlayer(identifier);
    }
}
