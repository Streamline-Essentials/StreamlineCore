package singularity.objects;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;

/**
 * Represents a Minecraft resource pack that can be pushed to players.
 *
 * <p>Instances hold all the data required by the Minecraft resource-pack
 * protocol: the download URL, the SHA-1 hash for integrity verification, an
 * optional prompt message, and a flag indicating whether acceptance is
 * mandatory.
 *
 * <p>Delivery is performed via {@link #sendPlayer(CosmicPlayer)}, which
 * delegates to the platform's native resource-pack API.
 */
@Setter
@Getter
public class CosmicResourcePack {

    /** The HTTPS URL from which clients will download the resource pack. */
    private String url;

    /**
     * The SHA-1 hash of the resource-pack ZIP, used by the client to verify
     * integrity.  Must be a 20-byte array.
     */
    private byte[] hash;

    /**
     * An optional prompt message shown to the player before they accept or
     * decline the pack.  May be {@code null} for no prompt.
     */
    private String prompt;

    /**
     * Whether the resource pack is required.  If {@code true} the player is
     * kicked if they decline.
     */
    private boolean force;

    /**
     * Constructs a new {@code CosmicResourcePack} with all required fields.
     *
     * @param url    the download URL of the resource pack; must not be
     *               {@code null}
     * @param hash   the 20-byte SHA-1 hash of the ZIP file
     * @param prompt the optional prompt text shown to the player, or
     *               {@code null}
     * @param force  {@code true} to kick the player if they decline
     */
    public CosmicResourcePack(String url, byte[] hash, String prompt, boolean force) {
        this.url = url;
        this.hash = hash;
        this.prompt = prompt;
        this.force = force;
    }

    /**
     * Sends this resource pack to the specified player through the platform's
     * native resource-pack mechanism.
     *
     * @param player the player to send the resource pack to; must not be
     *               {@code null}
     */
    public void sendPlayer(CosmicPlayer player) {
        Singularity.getInstance().getPlatform().sendResourcePack(this, player);
    }
}
