package singularity.data.update;

import singularity.database.servers.UpdateInfo;

import java.util.Optional;
import java.util.function.Function;

public interface UpdateFunction extends Function<String, Optional<UpdateInfo>> {
}
