package net.streamline.platform.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerTextures;
import singularity.utils.profiles.CosmicProfile;
import singularity.utils.profiles.PlayerLookup;
import singularity.utils.profiles.Textures;

import java.net.URL;
import java.util.Optional;
import java.util.UUID;

/**
 * Hands {@link PlayerLookup} the live profile of players online on this server, so their
 * names and skins never need a web lookup.
 */
public final class SpigotProfiles {
    private SpigotProfiles() {
    }

    public static void register() {
        PlayerLookup.setLocalSource(SpigotProfiles::profileOf);
    }

    private static Optional<CosmicProfile> profileOf(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return Optional.empty();
        boolean bedrock = PlayerLookup.isBedrock(uuid);

        try {
            // Paper exposes the textures property itself, with Mojang's signature.
            for (com.destroystokyo.paper.profile.ProfileProperty property : player.getPlayerProfile().getProperties()) {
                if (property.getName().equals("textures")) {
                    return Optional.of(new CosmicProfile(uuid, player.getName(), property.getValue(), property.getSignature(),
                            bedrock, CosmicProfile.Source.LOCAL));
                }
            }
        } catch (Throwable notPaper) {
            // Spigot only exposes the skin URL.
            try {
                PlayerTextures textures = player.getPlayerProfile().getTextures();
                URL skin = textures.getSkin();
                if (skin != null) {
                    return Optional.of(new CosmicProfile(uuid, player.getName(), Textures.forSkinUrl(skin.toString()), null,
                            bedrock, CosmicProfile.Source.LOCAL));
                }
            } catch (Throwable ignored) {
                // Before 1.18.1 there are no profiles.
            }
        }
        return Optional.of(new CosmicProfile(uuid, player.getName(), null, null, bedrock, CosmicProfile.Source.LOCAL));
    }
}
