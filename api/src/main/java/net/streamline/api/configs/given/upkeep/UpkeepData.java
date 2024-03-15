package net.streamline.api.configs.given.upkeep;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

@Getter
public class UpkeepData implements Comparable<UpkeepData> {
    private final String uuid;
    private final Date date;
    private final String serverUuid;

    public UpkeepData(String uuid, Date date, String serverUuid) {
        this.uuid = uuid;
        this.date = date;
        this.serverUuid = serverUuid;
    }

    @Override
    public int compareTo(@NotNull UpkeepData o) {
        if (uuid.compareTo(o.uuid) != 0) return uuid.compareTo(o.uuid);
        if (date.compareTo(o.date) != 0) return date.compareTo(o.date);
        return serverUuid.compareTo(o.serverUuid);
    }
}
