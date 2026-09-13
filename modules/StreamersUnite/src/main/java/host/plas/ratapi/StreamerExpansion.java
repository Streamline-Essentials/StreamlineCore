package host.plas.ratapi;

import host.plas.StreamersUnite;
import host.plas.data.LiveManager;
import singularity.placeholders.expansions.RATExpansion;
import singularity.placeholders.replaceables.IdentifiedUserReplaceable;

public class StreamerExpansion extends RATExpansion {
    public StreamerExpansion() {
        super(new RATExpansionBuilder("streamer"));
    }

    @Override
    public void init() {
        new IdentifiedUserReplaceable(this, "islive", (context, user) -> {
            return String.valueOf(LiveManager.isLive(user));
        }).register();

        new IdentifiedUserReplaceable(this, "prefix_liveoff", (context, user) -> {
            if (LiveManager.isLive(user)) {
                return StreamersUnite.getMainConfig().getLivePrefix();
            } else {
                return StreamersUnite.getMainConfig().getOfflinePrefix();
            }
        }).register();

        new IdentifiedUserReplaceable(this, "suffix_liveoff", (context, user) -> {
            if (LiveManager.isLive(user)) {
                return StreamersUnite.getMainConfig().getLiveSuffix();
            } else {
                return StreamersUnite.getMainConfig().getOfflineSuffix();
            }
        }).register();
    }
}
