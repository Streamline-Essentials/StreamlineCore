package singularity.placeholders.expansions;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import singularity.placeholders.RATRegistry;
import org.jetbrains.annotations.NotNull;

@Setter
@Getter
public abstract class RATExpansion {
    @Setter
    @Getter
    public static class RATExpansionBuilder {
        @NonNull
        private String identifier;
        @NonNull
        private String separator;
        @NonNull
        private String boundingPrefix;
        @NonNull
        private String boundingSuffix;

        public RATExpansionBuilder(@NonNull String identifier, @NonNull String separator, @NotNull String boundingPrefix, @NotNull String boundingSuffix) {
            this.identifier = identifier;
            this.separator = separator;
            this.boundingPrefix = boundingPrefix;
            this.boundingSuffix = boundingSuffix;
        }

        public RATExpansionBuilder(@NonNull String identifier, @NotNull String bounding) {
            this(identifier, "_", bounding, bounding);
        }

        public RATExpansionBuilder(@NonNull String identifier) {
            this(identifier, "%");
        }
    }

    @NonNull
    RATExpansionBuilder builder;

    public RATExpansion(@NonNull RATExpansionBuilder builder) {
        this.builder = builder;
        init();
    }

    public abstract void init();

    public void stop() {
        RATRegistry.unregisterAll(getBuilder().getIdentifier());
    }
}
