package singularity.permissions;

import singularity.data.players.CosmicPlayer;

import java.util.Optional;

public interface MetaGrabber {
    Optional<MetaValue> getPrefix(CosmicPlayer player);

    Optional<MetaValue> getSuffix(CosmicPlayer player);

    default Optional<MetaValue> getMeta(CosmicPlayer player, MetaKey key) {
        switch (key) {
            case PREFIX:
                return getPrefix(player);
            case SUFFIX:
                return getSuffix(player);
            default:
                return Optional.empty();
        }
    }

    void setMeta(MetaValue value);
}
