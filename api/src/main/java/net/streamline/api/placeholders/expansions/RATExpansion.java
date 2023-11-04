package net.streamline.api.placeholders.expansions;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.streamline.api.placeholders.RATRegistry;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class RATExpansion {
    @Getter
    public static class RATExpansionBuilder {
        @Setter @NonNull
        private String identifier;
        @Setter @NonNull
        private String separator;
        @Setter @NonNull
        private String boundingPrefix;
        @Setter @NonNull
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

    @Setter @NonNull
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
