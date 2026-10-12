package singularity.utils.profiles;

import lombok.Getter;

import java.util.Optional;
import java.util.UUID;

/**
 * A player's identity and skin as one of {@link PlayerLookup}'s sources reported it.
 *
 * <p>{@code texturesValue} is the base64 {@code textures} property Minecraft puts on game
 * profiles; {@code texturesSignature} is Mojang's signature of it, which only some sources
 * provide. Player heads render without a signature; skins applied to a player need one.</p>
 */
@Getter
public final class CosmicProfile {
    /** Where a profile came from. */
    public enum Source {
        /** A player online on this server, read from the platform or Floodgate. */
        LOCAL,
        /** playerdb.co. */
        PLAYERDB,
        /** api.ashcon.app. */
        ASHCON,
        /** GeyserMC's global API, for Bedrock players. */
        GEYSER,
    }

    private final UUID uuid;
    /** The player's name; for Bedrock players, their gamertag with the Floodgate prefix. */
    private final String name;
    private final String texturesValue;
    private final String texturesSignature;
    private final boolean bedrock;
    private final Source source;
    private final long fetchedAt;

    public CosmicProfile(UUID uuid, String name, String texturesValue, String texturesSignature, boolean bedrock, Source source) {
        this.uuid = uuid;
        this.name = name;
        this.texturesValue = blankToNull(texturesValue);
        this.texturesSignature = blankToNull(texturesSignature);
        this.bedrock = bedrock;
        this.source = source;
        this.fetchedAt = System.currentTimeMillis();
    }

    public boolean hasTextures() {
        return texturesValue != null;
    }

    /** The {@code textures.minecraft.net} URL of the skin, if the profile has one. */
    public Optional<String> getSkinUrl() {
        return Textures.skinUrl(texturesValue);
    }

    /** Whether the skin uses the slim (Alex) arm model. */
    public boolean isSlim() {
        return Textures.isSlim(texturesValue);
    }

    /** The cape's URL, if the profile has one. */
    public Optional<String> getCapeUrl() {
        return Textures.capeUrl(texturesValue);
    }

    /** A copy with {@code name} in place of this profile's name, when this one has none. */
    CosmicProfile withNameIfMissing(String fallback) {
        if (name != null && ! name.isEmpty()) return this;
        return new CosmicProfile(uuid, fallback, texturesValue, texturesSignature, bedrock, source);
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }
}
