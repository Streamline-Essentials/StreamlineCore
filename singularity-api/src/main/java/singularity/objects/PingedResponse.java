package singularity.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import singularity.data.uuid.UuidManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PingedResponse {
    @Getter @Setter
    private Protocol version;

    @Data
    @AllArgsConstructor
    public static class Protocol
    {

        private String name;
        private int protocol;
    }

    @Getter @Setter
    private Players players;

    @Data
    @AllArgsConstructor
    public static class Players
    {

        private int max;
        private int online;
        private PlayerInfo[] sample;
    }

    @Data
    @AllArgsConstructor
    public static class PlayerInfo
    {

        private String name;
        private UUID uniqueId;

        private static final UUID md5UUID = UUID.fromString(UuidManager.makeDashedUUID("af74a02d19cb445bb07f6866a861f783"));

        public PlayerInfo(String name, String id)
        {
            setName( name );
            setId( id );
        }

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

        public String getId()
        {
            return uniqueId.toString().replace( "-", "" );
        }
    }

    @Getter @Setter
    private String description;
    @Getter @Setter
    private CosmicFavicon favicon;

    @Data
    public static class ModInfo
    {

        private String type = "FML";
        private List<ModItem> modList = new ArrayList<>();
    }

    @Data
    @AllArgsConstructor
    public static class ModItem
    {

        private String modid;
        private String version;
    }

    // Right now, we don't get the mods from the user, so we just use a stock ModInfo object to
    // create the server ping. Vanilla clients will ignore this.
    private final ModInfo modinfo = new ModInfo();

    public PingedResponse(Protocol version, Players players, String description) {
        this(version, players, description, (String) null);
    }

    public PingedResponse(Protocol version, Players players, String description, String favicon)
    {
        this( version, players, description, favicon == null ? null : CosmicFavicon.create(favicon));
    }

    public PingedResponse(Protocol version, Players players, String description, CosmicFavicon favicon)
    {
        this.version = version;
        this.players = players;
        this.description = description;
        this.favicon = favicon;
    }

    public String getFaviconString()
    {
        return getFavicon() == null ? null : getFavicon().getEncoded();
    }

    public void setFaviconString(String favicon)
    {
        setFavicon(favicon == null ? null : CosmicFavicon.create( favicon ) );
    }
}
