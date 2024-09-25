package singularity.configs.given.punishments;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.UUID;

@Setter
@Getter
public class CosmicPunishment implements Comparable<CosmicPunishment> {
    private String idHash;
    private String punishedUUID;
    private String punisherUUID;
    private String reason;
    private PunishmentType type;
    private Date timeToUnpunish;

    public CosmicPunishment(String punishedUUID, String punisherUUID, String reason, PunishmentType type, Date timeToUnpunish) {
        String uuid = UUID.randomUUID().toString();
        this.idHash = uuid.substring(0, uuid.indexOf("-")).toUpperCase();
        this.punishedUUID = punishedUUID;
        this.punisherUUID = punisherUUID;
        this.reason = reason;
        this.type = type;
        this.timeToUnpunish = timeToUnpunish;
    }

    @Override
    public int compareTo(@NotNull CosmicPunishment o) {
        return this.idHash.compareTo(o.idHash);
    }
}
