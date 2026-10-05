package host.plas.collections.placeholders;

import gg.drak.thebase.utils.MatcherUtils;
import host.plas.collections.data.CollectionDefinition;
import host.plas.collections.data.CollectionManager;
import host.plas.collections.data.CollectionPlayer;
import singularity.data.console.CosmicSender;
import singularity.placeholders.expansions.RATExpansion;
import singularity.placeholders.replaceables.IdentifiedUserReplaceable;

import java.util.Optional;

/**
 * {@code %collections_unclaimed%}, {@code %collections_tiers%}, {@code %collections_maxed%},
 * {@code %collections_amount_<collection>%} and {@code %collections_level_<collection>%}, for
 * players loaded on this server.
 */
public class CollectionsExpansion extends RATExpansion {
    public CollectionsExpansion() {
        super(new RATExpansionBuilder("collections"));
    }

    @Override
    public void init() {
        new IdentifiedUserReplaceable(this, "unclaimed", (s, user) ->
                progress(user).map(p -> String.valueOf(CollectionManager.unclaimed(p))).orElse("0")).register();

        new IdentifiedUserReplaceable(this, "tiers", (s, user) -> progress(user).map(p -> {
            int tiers = 0;
            for (CollectionDefinition definition : CollectionManager.getCatalog().all()) {
                tiers += definition.tier(p.amount(definition.getId()));
            }
            return String.valueOf(tiers);
        }).orElse("0")).register();

        new IdentifiedUserReplaceable(this, "maxed", (s, user) -> progress(user).map(p -> {
            int maxed = 0;
            for (CollectionDefinition definition : CollectionManager.getCatalog().all()) {
                if (definition.maxed(p.amount(definition.getId()))) maxed++;
            }
            return String.valueOf(maxed);
        }).orElse("0")).register();

        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("amount_") + "(.*?)", 1, (s, user) -> {
            Optional<CollectionDefinition> definition = CollectionManager.getCatalog().get(s.get());
            if (definition.isEmpty()) return s.string();
            return progress(user).map(p -> String.valueOf(p.amount(definition.get().getId()))).orElse("0");
        }).register();

        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("level_") + "(.*?)", 1, (s, user) -> {
            Optional<CollectionDefinition> definition = CollectionManager.getCatalog().get(s.get());
            if (definition.isEmpty()) return s.string();
            return progress(user).map(p -> String.valueOf(definition.get().tier(p.amount(definition.get().getId())))).orElse("0");
        }).register();
    }

    private static Optional<CollectionPlayer> progress(CosmicSender user) {
        if (user == null || user.isConsole()) return Optional.empty();
        return CollectionManager.getLoaded(user.getUuid());
    }
}
