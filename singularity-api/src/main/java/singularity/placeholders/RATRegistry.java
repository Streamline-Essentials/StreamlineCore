package singularity.placeholders;

import gg.drak.thebase.objects.AtomicString;
import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleLike;
import singularity.modules.CosmicModule;
import singularity.placeholders.callbacks.RATCallback;
import singularity.placeholders.expansions.RATExpansion;
import singularity.placeholders.replaceables.*;
import singularity.utils.UserUtils;

import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Central registry for all RAT (Replace-And-Transform) placeholder replaceables.
 *
 * <p>Replaceables are stored in a thread-safe {@link ConcurrentSkipListMap} keyed
 * by the regex pattern of their {@link singularity.placeholders.handling.RATHandledString}.
 * The registry exposes static methods to register, unregister, and resolve
 * placeholders within arbitrary strings.
 *
 * <p>Two resolution strategies are provided:
 * <ul>
 *   <li>{@link #fetch(String)} — resolves context-free placeholders once.</li>
 *   <li>{@link #fetchDirty(String)} — resolves recursively until the string
 *       stabilises (no further changes occur).</li>
 *   <li>{@link #fetch(String, CosmicSender)} — resolves user-aware placeholders
 *       first, then falls back to context-free resolution.</li>
 * </ul>
 */
public class RATRegistry {

    /**
     * The live map of regex pattern → {@link AbstractReplaceable} entries.
     * This map is replaced wholesale by {@link #unregisterAll()}.
     */
    @Getter @Setter
    private static ConcurrentSkipListMap<String, AbstractReplaceable<? extends RATCallback>> replacements = new ConcurrentSkipListMap<>();

    /**
     * Registers the given replaceable, keyed by its regex pattern.
     * Any existing entry with the same pattern is overwritten.
     *
     * @param replacement the replaceable to register; must not be {@code null}
     */
    public static void register(AbstractReplaceable<? extends RATCallback> replacement) {
        replacements.put(replacement.getHandledString().getRegex(), replacement);
    }

    /**
     * Removes the given replaceable from the registry using its regex pattern as the key.
     *
     * @param replacement the replaceable to remove
     */
    public static void unregister(AbstractReplaceable<? extends RATCallback> replacement) {
        replacements.remove(replacement.getHandledString().getRegex());
    }

    /**
     * Removes the replaceable whose regex pattern equals the given string.
     *
     * @param from the exact regex pattern key to remove
     */
    public static void unregister(String from) {
        replacements.remove(from);
    }

    /**
     * Clears all registered replaceables, replacing the internal map with a
     * new empty instance.
     */
    public static void unregisterAll() {
        replacements = new ConcurrentSkipListMap<>();
    }

    /**
     * Removes all replaceables that were registered by the given module.
     *
     * <p>A replaceable is considered to belong to the module if it is an instance
     * of {@link ModuleReplaceable} or {@link UserModuleReplaceable} and its
     * {@code getModule()} returns the supplied {@link ModuleLike}.
     *
     * @param ModuleLike the module whose replaceables should be removed
     */
    public static void unregisterAll(ModuleLike ModuleLike) {
        replacements.values().removeIf(replacement -> replacement instanceof ModuleReplaceable && ((ModuleReplaceable) replacement).getModule().equals(ModuleLike) ||
                replacement instanceof UserModuleReplaceable && ((UserModuleReplaceable) replacement).getModule().equals(ModuleLike));
    }

    /**
     * Removes all replaceables whose identifier equals the given string.
     *
     * <p>A replaceable is matched if it is an instance of
     * {@link IdentifiedReplaceable} or {@link IdentifiedUserReplaceable} and its
     * {@code getIdentifier()} equals {@code identifier}.
     *
     * @param identifier the expansion or namespace identifier to match
     */
    public static void unregisterAll(String identifier) {
        replacements.values().removeIf(replacement -> replacement instanceof IdentifiedReplaceable && ((IdentifiedReplaceable) replacement).getIdentifier().equals(identifier) ||
                replacement instanceof IdentifiedUserReplaceable && ((IdentifiedUserReplaceable) replacement).getIdentifier().equals(identifier));
    }

    /**
     * Removes all replaceables whose regex pattern begins with the given prefix.
     *
     * @param startsWith the prefix string to match against each replaceable's regex
     */
    public static void unregisterAllStartsWith(String startsWith) {
        replacements.values().removeIf(replacements -> replacements.getHandledString().getRegex().startsWith(startsWith));
    }

    /**
     * Retrieves the replaceable registered under the given regex pattern key,
     * or {@code null} if no such entry exists.
     *
     * @param from the exact regex pattern key to look up
     * @return the associated {@link AbstractReplaceable}, or {@code null}
     */
    public static AbstractReplaceable<? extends RATCallback> getReplacement(String from) {
        return replacements.get(from);
    }

    /**
     * Resolves all context-free (non-user-aware) placeholders in the given string.
     *
     * <p>Each {@link GenericReplaceable} is applied sequentially. User-aware
     * replaceables ({@link UserReplaceable}) are resolved against the console
     * sender as a fallback.
     *
     * @param from the input string potentially containing placeholder tokens
     * @return the input string with all applicable placeholders replaced
     */
    public static String fetch(String from) {
        AtomicString result = new AtomicString(from);
        getReplacements().forEach((s, replacement) -> {
            if (replacement == null) return;
            if (! replacement.isReplaceWorthy()) return;
            if (replacement instanceof GenericReplaceable) {
                GenericReplaceable generic = (GenericReplaceable) replacement;
                result.set(generic.fetch(result.get()));
            }
            if (replacement instanceof UserReplaceable) {
                UserReplaceable user = (UserReplaceable) replacement;
                result.set(user.fetchAs(result.get(), UserUtils.getConsole()));
            }
        });

        return result.get();
    }

    /**
     * Repeatedly resolves context-free placeholders in the given string until
     * the result stabilises (i.e. no further replacements occur).
     *
     * <p>This handles nested or self-referential placeholders that introduce new
     * placeholder tokens after an initial substitution.
     *
     * @param from the input string to resolve
     * @return the fully-resolved string with no remaining replaceable tokens
     */
    public static String fetchDirty(String from) {
        String temp = from;
        temp = fetch(temp);
        while (! Objects.equals(temp, from)) {
            from = temp;
            temp = fetch(temp);
        }
        return temp;
    }

    /**
     * Resolves all user-aware placeholders in the given string for the specified
     * sender, then applies the standard context-free resolution pass.
     *
     * @param from the input string potentially containing placeholder tokens
     * @param user the sender used to contextualise user-aware placeholders
     * @return the input string with all applicable placeholders replaced
     */
    public static String fetch(String from, CosmicSender user) {
        AtomicString result = new AtomicString(from);
        getReplacements().forEach((s, replacement) -> {
            if (replacement instanceof UserReplaceable) {
                UserReplaceable userReplacement = (UserReplaceable) replacement;
                result.set(userReplacement.fetchAs(result.get(), user));
            }
        });
        result.set(fetch(result.get()));

        return result.get();
    }

    /**
     * Repeatedly resolves user-aware and context-free placeholders in the given
     * string for the specified sender until the result stabilises.
     *
     * @param from the input string to resolve
     * @param user the sender used to contextualise user-aware placeholders
     * @return the fully-resolved string with no remaining replaceable tokens
     */
    public static String fetchDirty(String from, CosmicSender user) {
        String temp = from;
        temp = fetch(temp, user);
        while (! Objects.equals(temp, from)) {
            from = temp;
            temp = fetch(temp, user);
        }
        return temp;
    }

    /**
     * Returns the default scoped placeholder key for the given base name within
     * the provided module, formatted as {@code <moduleId>_<from>}.
     *
     * @param from   the base placeholder key
     * @param module the module providing the namespace
     * @return the module-scoped placeholder key string
     */
    public static String getModuledDefault(String from, CosmicModule module) {
        return module.getIdentifier() + "_" + from;
    }

    /**
     * Builds a regex pattern that matches placeholders from the given expansion
     * by wrapping the supplied regex with the expansion's bounding characters,
     * identifier, and separator (all made regex-literal).
     *
     * <p>For example, given expansion {@code %myexp%} with separator {@code _}
     * and the regex {@code \\w+}, the result would be
     * {@code \%myexp\_\w+\%} (with actual escaping applied).
     *
     * @param regex     the inner regex for the placeholder key portion
     * @param expansion the expansion whose formatting configuration is used
     * @return the fully-qualified regex pattern string
     */
    public static String getRegexWithExpansion(String regex, RATExpansion expansion) {
        return MatcherUtils.makeLiteral(expansion.getBuilder().getBoundingPrefix() + expansion.getBuilder().getIdentifier() + expansion.getBuilder().getSeparator())
                + regex + MatcherUtils.makeLiteral(expansion.getBuilder().getBoundingSuffix());
    }

    /**
     * Builds a literal placeholder string for the given key within the specified
     * expansion, suitable for embedding directly in text.
     *
     * <p>For example, with expansion {@code %myexp%} and literal {@code mykey},
     * this returns {@code %myexp_mykey%}.
     *
     * @param literal   the placeholder key to embed
     * @param expansion the expansion whose formatting configuration is used
     * @return the fully-formatted placeholder string
     */
    public static String getLiteralWithExpansion(String literal, RATExpansion expansion) {
        return expansion.getBuilder().getBoundingPrefix() + expansion.getBuilder().getIdentifier()
                + expansion.getBuilder().getSeparator() + literal + expansion.getBuilder().getBoundingSuffix();
    }
}
