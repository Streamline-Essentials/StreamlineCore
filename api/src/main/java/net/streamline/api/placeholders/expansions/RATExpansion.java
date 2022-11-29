package net.streamline.api.placeholders.expansions;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.streamline.api.placeholders.RATRegistry;
import org.jetbrains.annotations.NotNull;

public abstract class RATExpansion {
    public static class RATExpansionBuilder {
        @Getter @Setter @NonNull
        private String identifier;
        @Getter @Setter @NonNull
        private String boundingPrefix;
        @Getter @Setter @NonNull
        private String boundingSuffix;

        public RATExpansionBuilder(String identifier, @NotNull String boundingPrefix, @NotNull String boundingSuffix) {
            this.identifier = identifier;
            this.boundingPrefix = boundingPrefix;
            this.boundingSuffix = boundingSuffix;
        }

        public RATExpansionBuilder(String identifier, @NotNull String bounding) {
            this(identifier, bounding, bounding);
        }

        public RATExpansionBuilder(String identifier) {
            this(identifier, "%");
        }
    }

    @Getter @Setter @NonNull
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
