package host.plas.collections.placeholders;

import gg.drak.thebase.utils.MatcherUtils;
import host.plas.collections.data.CollectionDefinition;
import host.plas.collections.data.CollectionManager;
import host.plas.collections.data.TrackedPlayer;
import host.plas.collections.progression.ProgressionManager;
import host.plas.collections.progression.ProgressionStat;
import host.plas.collections.progression.Skill;
import host.plas.collections.progression.StarsFormula;
import host.plas.collections.slayer.SlayerCatalog;
import host.plas.collections.slayer.SlayerDefinition;
import host.plas.collections.slayer.SlayerManager;
import singularity.data.console.CosmicSender;
import singularity.placeholders.expansions.RATExpansion;
import singularity.placeholders.replaceables.IdentifiedUserReplaceable;

import java.util.Optional;
import java.util.function.Function;

/**
 * The module's placeholders, for players loaded on this server. Values come from memory, never the
 * database, so they are cheap enough for chat formats. Registered as {@code streamlinecollections}
 * and, for older configs, {@code collections}.
 *
 * <ul>
 *     <li>Collections: {@code unclaimed}, {@code tiers}, {@code maxed}, {@code amount_<collection>},
 *     {@code level_<collection>}.</li>
 *     <li>Slayer: {@code slayer_kills}, {@code slayer_unclaimed}, {@code slayer_tiers}, {@code slayer_maxed},
 *     {@code slayer_kills_<mob>}, {@code slayer_level_<mob>}.</li>
 *     <li>Progression: {@code stars} (the chat prefix, empty when chat stars are off), {@code star_emoji},
 *     {@code star_level}, {@code star_exp}, {@code star_exp_<category>}, {@code star_progress} (percent to
 *     the next star), {@code skill_level_<skill>}, {@code skill_exp_<skill>}.</li>
 * </ul>
 */
public class CollectionsExpansion extends RATExpansion {
    public CollectionsExpansion(String identifier) {
        super(new RATExpansionBuilder(identifier));
    }

    @Override
    public void init() {
        registerCollections();
        registerSlayer();
        registerProgression();
    }

    private void registerCollections() {
        literal("unclaimed", user -> collections(user).map(p -> String.valueOf(CollectionManager.unclaimed(p))).orElse("0"));
        literal("tiers", user -> collections(user).map(p -> {
            int tiers = 0;
            for (CollectionDefinition definition : CollectionManager.getCatalog().all()) tiers += definition.tier(p.amount(definition.getId()));
            return String.valueOf(tiers);
        }).orElse("0"));
        literal("maxed", user -> collections(user).map(p -> {
            int maxed = 0;
            for (CollectionDefinition definition : CollectionManager.getCatalog().all()) {
                if (definition.maxed(p.amount(definition.getId()))) maxed++;
            }
            return String.valueOf(maxed);
        }).orElse("0"));

        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("amount_") + "(.*?)", 1, (s, user) -> {
            Optional<CollectionDefinition> definition = CollectionManager.getCatalog().get(s.get());
            if (definition.isEmpty()) return s.string();
            return collections(user).map(p -> String.valueOf(p.amount(definition.get().getId()))).orElse("0");
        }).register();
        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("level_") + "(.*?)", 1, (s, user) -> {
            Optional<CollectionDefinition> definition = CollectionManager.getCatalog().get(s.get());
            if (definition.isEmpty()) return s.string();
            return collections(user).map(p -> String.valueOf(definition.get().tier(p.amount(definition.get().getId())))).orElse("0");
        }).register();
    }

    private void registerSlayer() {
        literal("slayer_kills", user -> slayer(user)
                .map(p -> String.valueOf(SlayerManager.Totals.of(p, SlayerCatalog.all()).getKills())).orElse("0"));
        literal("slayer_unclaimed", user -> slayer(user).map(p -> String.valueOf(SlayerManager.unclaimed(p))).orElse("0"));
        literal("slayer_tiers", user -> slayer(user)
                .map(p -> String.valueOf(SlayerManager.Totals.of(p, SlayerCatalog.all()).getTiers())).orElse("0"));
        literal("slayer_maxed", user -> slayer(user)
                .map(p -> String.valueOf(SlayerManager.Totals.of(p, SlayerCatalog.all()).getMaxed())).orElse("0"));

        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("slayer_kills_") + "(.*?)", 1, (s, user) -> {
            SlayerDefinition definition = SlayerCatalog.get(s.get());
            if (definition == null) return s.string();
            return slayer(user).map(p -> String.valueOf(p.amount(definition.getId()))).orElse("0");
        }).register();
        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("slayer_level_") + "(.*?)", 1, (s, user) -> {
            SlayerDefinition definition = SlayerCatalog.get(s.get());
            if (definition == null) return s.string();
            return slayer(user).map(p -> String.valueOf(definition.tier(p.amount(definition.getId())))).orElse("0");
        }).register();
    }

    private void registerProgression() {
        literal("stars", ProgressionManager::chatPrefix);
        literal("star_emoji", user -> ProgressionManager.message("star-emoji"));
        literal("star_level", user -> progression(user).map(p -> String.valueOf(ProgressionManager.stars(p))).orElse("0"));
        literal("star_exp", user -> progression(user).map(p -> ProgressionManager.format(ProgressionManager.totalXp(p))).orElse("0"));
        literal("star_progress", user -> progression(user)
                .map(p -> String.valueOf(Math.round(StarsFormula.progressToNextStar(ProgressionManager.totalXp(p)) * 100))).orElse("0"));

        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("star_exp_") + "(.*?)", 1, (s, user) -> {
            ProgressionStat stat = ProgressionStat.byKey(s.get());
            if (stat == null) return s.string();
            return progression(user).map(p -> ProgressionManager.format(p.amount(stat.key()))).orElse("0");
        }).register();
        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("skill_level_") + "(.*?)", 1, (s, user) -> {
            Skill skill = Skill.byName(s.get());
            if (skill == null) return s.string();
            return progression(user).map(p -> String.valueOf(ProgressionManager.skillLevel(p, skill))).orElse("0");
        }).register();
        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("skill_exp_") + "(.*?)", 1, (s, user) -> {
            Skill skill = Skill.byName(s.get());
            if (skill == null) return s.string();
            return progression(user).map(p -> ProgressionManager.format(ProgressionManager.skillXp(p, skill))).orElse("0");
        }).register();
    }

    private void literal(String key, Function<CosmicSender, String> value) {
        new IdentifiedUserReplaceable(this, key, (s, user) -> value.apply(user)).register();
    }

    private static Optional<TrackedPlayer> collections(CosmicSender user) {
        if (user == null || user.isConsole()) return Optional.empty();
        return CollectionManager.getLoaded(user.getUuid());
    }

    private static Optional<TrackedPlayer> slayer(CosmicSender user) {
        if (user == null || user.isConsole()) return Optional.empty();
        return SlayerManager.getLoaded(user.getUuid());
    }

    private static Optional<TrackedPlayer> progression(CosmicSender user) {
        if (user == null || user.isConsole()) return Optional.empty();
        return ProgressionManager.getLoaded(user.getUuid());
    }
}
