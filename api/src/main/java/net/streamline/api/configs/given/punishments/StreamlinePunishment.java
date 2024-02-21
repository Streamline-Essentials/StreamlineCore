package net.streamline.api.configs.given.punishments;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.UUID;

@Setter
@Getter
public class StreamlinePunishment implements Comparable<StreamlinePunishment> {
    private String idHash;
    private String punishedUUID;
    private String punisherUUID;
    private String reason;
    private PunishmentType type;
    private Date timeToUnpunish;

    public StreamlinePunishment(String punishedUUID, String punisherUUID, String reason, PunishmentType type, Date timeToUnpunish) {
        String uuid = UUID.randomUUID().toString();
        this.idHash = uuid.substring(0, uuid.indexOf("-")).toUpperCase();
        this.punishedUUID = punishedUUID;
        this.punisherUUID = punisherUUID;
        this.reason = reason;
        this.type = type;
        this.timeToUnpunish = timeToUnpunish;
    }

    @Override
    public int compareTo(@NotNull StreamlinePunishment o) {
        return this.idHash.compareTo(o.idHash);
    }
}
