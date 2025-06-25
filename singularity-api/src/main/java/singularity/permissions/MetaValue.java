package singularity.permissions;

import gg.drak.thebase.objects.Identified;
import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;
import singularity.utils.UserUtils;

import java.util.Optional;

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

    public Optional<CosmicPlayer> getOn() {
        return UserUtils.getOrCreatePlayer(identifier);
    }
}
