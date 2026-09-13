package host.plas.managers;

import singularity.data.console.CosmicSender;
import singularity.data.uuid.UuidInfo;
import singularity.data.uuid.UuidManager;
import singularity.utils.UserUtils;

import java.util.Optional;

public class StreamerUtils {
    public static Optional<CosmicSender> getOrGetSender(String uuid) {
        Optional<UuidInfo> optional = UuidManager.getUuid(uuid);
        return optional.filter(r -> {
            boolean accept = false;

            for (String u : r.getNames()) {
                if (u != null && ! u.isBlank()) {
                    accept = true;
                    break;
                }
            }

            return accept;
        }).flatMap(r -> UserUtils.getOrCreateSender(r.getUuid()));
    }

    public static Optional<CosmicSender> getOrGetSenderByName(String name) {
        Optional<String> optional = UuidManager.getUuidFromName(name);
        return optional.filter(u -> {
            boolean accept = false;

            if (! u.isBlank()) {
                accept = true;
            }

            return accept;
        }).flatMap(UserUtils::getOrCreateSender);
    }
}
