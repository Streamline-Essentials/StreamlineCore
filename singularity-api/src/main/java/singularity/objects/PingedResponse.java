package singularity.objects;

import gg.drak.thebase.lib.leonhard.storage.shaded.jetbrains.annotations.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import singularity.data.uuid.UuidManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents the full payload returned in response to a Minecraft server-list
 * ping (status) request.
 *
 * <p>The response contains the server's protocol version, player count
 * information (including an optional sample of online player names), a
 * description (MOTD), an optional favicon, and a Forge mod-info block for
 * FML-compatible clients.
 *
 * <p>Instances are constructed via the provided constructors and are then
 * serialised to JSON by the platform's ping handler.
 */
public class PingedResponse {

    /**
     * The protocol version information reported to connecting clients.
     * Controls the version string displayed in the server browser when the
     * client's version does not match.
     */
    @Getter @Setter
    private Protocol version;

    /**
     * Protocol version descriptor sent inside the server-list ping response.
     */
    @Data
    @AllArgsConstructor
    public static class Protocol
    {
        /** The human-readable version name (e.g. {@code "1.20.1"}). */
        private String name;

        /**
         * The numeric Minecraft protocol version number (e.g. {@code 763} for
         * 1.20.1).
         */
        private int protocol;
    }

    /**
     * Player count and sample information included in the ping response.
     */
    @Getter @Setter
    private Players players;

    /**
     * Aggregated player-count information for the server-list display.
     */
    @Data
    @AllArgsConstructor
    public static class Players
    {
        /** The maximum number of players the server allows simultaneously. */
        private int max;

        /** The number of players currently online. */
        private int online;

        /**
         * A sample of online players shown when hovering over the player count
         * in the server browser.  May be {@code null} or empty.
         */
        private PlayerInfo[] sample;
    }

    /**
     * A single player entry within the hover sample shown on the server list.
     */
    @Data
    @AllArgsConstructor
    public static class PlayerInfo
    {
        /** The display name of this player entry. */
        private String name;

        /** The unique identifier of this player entry. */
        private UUID uniqueId;

        /**
         * Fallback UUID used when the provided ID string cannot be parsed into
         * a valid {@link UUID}.
         */
        private static final UUID md5UUID = UUID.fromString(UuidManager.makeDashedUUID("af74a02d19cb445bb07f6866a861f783"));

        /**
         * Constructs a {@code PlayerInfo} from a display name and a raw
         * (undashed) UUID string.
         *
         * @param name the player's display name
         * @param id   the undashed UUID string; falls back to
         *             {@link #md5UUID} if invalid
         */
        public PlayerInfo(String name, String id)
        {
            setName( name );
            setId( id );
        }

        /**
         * Sets the unique identifier from a raw (undashed) UUID string.  If
         * the string cannot be parsed as a valid UUID, the fallback
         * {@link #md5UUID} is used instead.
         *
         * @param id the undashed UUID string to parse
         */
        public void setId(String id)
        {
            try
            {
                uniqueId = UUID.fromString(UuidManager.makeDashedUUID(id));
            } catch ( Exception e )
            {
                // Fallback on a valid uuid otherwise Minecraft complains
                uniqueId = md5UUID;
            }
        }

        /**
         * Returns the unique identifier as an undashed UUID string.
         *
         * @return the UUID without dashes
         */
        public String getId()
        {
            return uniqueId.toString().replace( "-", "" );
        }
    }

    /**
     * The MOTD (Message of the Day) displayed beneath the server name in the
     * server browser.  Supports legacy colour codes.
     */
    @Getter @Setter
    private String description;

    /**
     * The optional 64×64 favicon displayed next to the server in the server
     * browser.  {@code null} when no favicon is configured.
     */
    @Getter @Setter @Nullable
    private CosmicFavicon favicon;

    /**
     * Forge Mod Loader metadata included so that FML clients can detect this
     * server.  Vanilla clients ignore this field.
     */
    @Data
    public static class ModInfo
    {
        /** The mod loader type identifier; always {@code "FML"} for Forge. */
        private String type = "FML";

        /** The list of mods present on this server. */
        private List<ModItem> modList = new ArrayList<>();
    }

    /**
     * A single mod entry in the FML mod-info block.
     */
    @Data
    @AllArgsConstructor
    public static class ModItem
    {
        /** The mod ID string (e.g. {@code "forge"}). */
        private String modid;

        /** The version string of the mod (e.g. {@code "14.23.5.2860"}). */
        private String version;
    }

    // Right now, we don't get the mods from the user, so we just use a stock ModInfo object to
    // create the server ping. Vanilla clients will ignore this.
    /**
     * The Forge mod-info block included in the ping response.  Vanilla clients
     * ignore this.  Currently always uses a default empty {@link ModInfo}.
     */
    private final ModInfo modinfo = new ModInfo();

    /**
     * Constructs a ping response without a favicon.
     *
     * @param version     the protocol version descriptor
     * @param players     the player count information
     * @param description the server MOTD
     * @throws IOException never thrown by this constructor; declared for
     *                     API compatibility with the favicon overloads
     */
    public PingedResponse(Protocol version, Players players, String description) throws IOException {
        this(version, players, description, (String) null);
    }

    /**
     * Constructs a ping response, downloading the favicon from the given URL
     * string.  If the URL is {@code null} or the download fails, the favicon
     * is omitted.
     *
     * @param version     the protocol version descriptor
     * @param players     the player count information
     * @param description the server MOTD
     * @param favicon     the URL string of the favicon image, or {@code null}
     * @throws IOException never thrown by this constructor; declared for
     *                     API compatibility
     */
    public PingedResponse(Protocol version, Players players, String description, String favicon) throws IOException {
        this(version, players, description, favicon == null ? null : CosmicFavicon.createFromURL(favicon));
    }

    /**
     * Constructs a ping response with a pre-built {@link CosmicFavicon}.
     *
     * @param version     the protocol version descriptor
     * @param players     the player count information
     * @param description the server MOTD
     * @param favicon     the favicon to include, or {@code null} for none
     */
    public PingedResponse(Protocol version, Players players, String description, CosmicFavicon favicon)
    {
        this.version = version;
        this.players = players;
        this.description = description;
        this.favicon = favicon;
    }

    /**
     * Returns the Base64 data-URI string of the favicon, or {@code null} if
     * no favicon is set.
     *
     * @return the encoded favicon string, or {@code null}
     */
    public String getFaviconString()
    {
        return getFavicon() == null ? null : getFavicon().getEncoded();
    }
}
