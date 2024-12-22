package singularity.database.servers;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

@Getter @Setter
public class UpdateInfo implements Comparable<UpdateInfo> {
    private Date date;
    private String serverUuid;

    public UpdateInfo(Date date, String serverUuid) {
        setDate(date);
        setServerUuid(serverUuid);
    }

    @Override
    public int compareTo(@NotNull UpdateInfo o) {
        return getDate().compareTo(o.getDate());
    }
}
