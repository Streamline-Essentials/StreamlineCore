package singularity.placeholders.expansions;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import singularity.placeholders.RATRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all RAT (Replace-And-Transform) placeholder expansions.
 *
 * <p>A {@code RATExpansion} groups a set of related placeholder replaceables
 * under a common identifier, separator, and bounding characters. Subclasses
 * register their placeholders during {@link #init()} and can deregister them
 * all at once via {@link #stop()}.
 *
 * <p>Typical bounding characters are {@code %} (e.g. {@code %expansion_key%}),
 * but any string can be used.
 */
@Setter
@Getter
public abstract class RATExpansion {

    /**
     * Builder / configuration object that describes how placeholder strings are
     * formatted for a particular {@link RATExpansion}.
     *
     * <p>A fully-specified expansion string has the form:
     * <pre>
     *   {boundingPrefix}{identifier}{separator}{key}{boundingSuffix}
     * </pre>
     * For example, with the defaults: {@code %myexpansion_somekey%}.
     */
    @Setter
    @Getter
    public static class RATExpansionBuilder {

        /**
         * The unique identifier for this expansion (e.g. {@code "myexpansion"}).
         * Used as the namespace for all placeholders it registers.
         */
        @NonNull
        private String identifier;

        /**
         * The separator placed between the expansion identifier and the
         * placeholder key (defaults to {@code "_"}).
         */
        @NonNull
        private String separator;

        /**
         * The string placed before the identifier in a placeholder token
         * (e.g. {@code "%"}).
         */
        @NonNull
        private String boundingPrefix;

        /**
         * The string placed after the key in a placeholder token
         * (e.g. {@code "%"}).
         */
        @NonNull
        private String boundingSuffix;

        /**
         * Constructs a fully-specified builder.
         *
         * @param identifier     the expansion's namespace identifier
         * @param separator      the separator between identifier and key
         * @param boundingPrefix the prefix bounding character(s)
         * @param boundingSuffix the suffix bounding character(s)
         */
        public RATExpansionBuilder(@NonNull String identifier, @NonNull String separator, @NotNull String boundingPrefix, @NotNull String boundingSuffix) {
            this.identifier = identifier;
            this.separator = separator;
            this.boundingPrefix = boundingPrefix;
            this.boundingSuffix = boundingSuffix;
        }

        /**
         * Constructs a builder with a symmetric bounding string and the default
         * separator ({@code "_"}).
         *
         * @param identifier the expansion's namespace identifier
         * @param bounding   the string used as both prefix and suffix (e.g. {@code "%"})
         */
        public RATExpansionBuilder(@NonNull String identifier, @NotNull String bounding) {
            this(identifier, "_", bounding, bounding);
        }

        /**
         * Constructs a builder using the conventional {@code %} bounding and
         * the default separator ({@code "_"}).
         *
         * @param identifier the expansion's namespace identifier
         */
        public RATExpansionBuilder(@NonNull String identifier) {
            this(identifier, "%");
        }
    }

    /**
     * The configuration builder that defines this expansion's identifier,
     * separator, and bounding characters.
     */
    @NonNull
    RATExpansionBuilder builder;

    /**
     * Constructs a new {@code RATExpansion} with the given builder configuration
     * and immediately calls {@link #init()} to allow the subclass to register
     * its placeholders.
     *
     * @param builder the configuration builder; must not be {@code null}
     */
    public RATExpansion(@NonNull RATExpansionBuilder builder) {
        this.builder = builder;
        init();
    }

    /**
     * Called during construction to allow subclasses to register their
     * placeholder replaceables with {@link RATRegistry}.
     *
     * <p>Implementations should use {@link RATRegistry#register} (or the
     * convenience methods on {@link RATRegistry}) to add their replaceables.
     */
    public abstract void init();

    /**
     * Unregisters all placeholders that belong to this expansion from the
     * {@link RATRegistry}, identified by this expansion's
     * {@link RATExpansionBuilder#getIdentifier() identifier}.
     */
    public void stop() {
        RATRegistry.unregisterAll(getBuilder().getIdentifier());
    }
}
