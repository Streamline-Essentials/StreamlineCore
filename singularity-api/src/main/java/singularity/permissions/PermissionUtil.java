package singularity.permissions;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.CosmicPlayer;

import java.util.Optional;

public class PermissionUtil {
    @Getter @Setter
    private static Optional<MetaGrabber> optionalMetaGrabber = Optional.empty();

    public static Optional<MetaValue> getMeta(CosmicPlayer player, MetaKey key) {
        return optionalMetaGrabber.map(metaGrabber -> metaGrabber.getMeta(player, key)).filter(Optional::isPresent).map(Optional::get);
    }

    public static Optional<MetaValue> getPrefix(CosmicPlayer player) {
        return getMeta(player, MetaKey.PREFIX);
    }

    public static Optional<MetaValue> getSuffix(CosmicPlayer player) {
        return getMeta(player, MetaKey.SUFFIX);
    }

    public static void setMeta(MetaValue value) {
        optionalMetaGrabber.ifPresent(metaGrabber -> metaGrabber.setMeta(value));
    }

    public static void setPrefix(CosmicPlayer player, String prefix, long duration, int priority) {
        setMeta(new MetaValue(player.getIdentifier(), MetaKey.PREFIX, prefix, duration, priority));
    }

    public static void setSuffix(CosmicPlayer player, String suffix, long duration, int priority) {
        setMeta(new MetaValue(player.getIdentifier(), MetaKey.SUFFIX, suffix, duration, priority));
    }

    public static void setMetaGrabber(MetaGrabber metaGrabber) {
        optionalMetaGrabber = Optional.of(metaGrabber);
    }

    public static void removeMetaGrabber() {
        optionalMetaGrabber = Optional.empty();
    }
}
