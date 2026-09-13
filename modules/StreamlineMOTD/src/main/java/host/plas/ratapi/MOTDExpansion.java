package host.plas.ratapi;

import singularity.placeholders.expansions.RATExpansion;
import singularity.placeholders.replaceables.IdentifiedReplaceable;
import host.plas.StreamlineMOTD;

public class MOTDExpansion extends RATExpansion {
    public MOTDExpansion() {
        super(new RATExpansionBuilder("motd"));
    }

    @Override
    public void init() {
        new IdentifiedReplaceable(this, "motd_text", (s) -> String.valueOf(StreamlineMOTD.getConfig().getDescription())).register();
        new IdentifiedReplaceable(this, "motd_index", (s) -> String.valueOf(StreamlineMOTD.getConfig().getCurrentMotd())).register();
        new IdentifiedReplaceable(this, "motd_ticks_max", (s) -> String.valueOf(StreamlineMOTD.getConfig().getMotdTicks())).register();

        new IdentifiedReplaceable(this, "sample_text", (s) -> String.valueOf(StreamlineMOTD.getConfig().getCurrentSampleText())).register();
        new IdentifiedReplaceable(this, "sample_index", (s) -> String.valueOf(StreamlineMOTD.getConfig().getCurrentSample())).register();
        new IdentifiedReplaceable(this, "sample_ticks_max", (s) -> String.valueOf(StreamlineMOTD.getConfig().getSampleTicks())).register();

        new IdentifiedReplaceable(this, "max", (s) -> String.valueOf(StreamlineMOTD.getConfig().getMaxPlayersValue())).register();
        new IdentifiedReplaceable(this, "online", (s) -> String.valueOf(StreamlineMOTD.getConfig().getOnlinePlayersValue())).register();

        new IdentifiedReplaceable(this, "protocol_name", (s) -> String.valueOf(StreamlineMOTD.getConfig().versionName())).register();
        new IdentifiedReplaceable(this, "protocol_version", (s) -> String.valueOf(StreamlineMOTD.getConfig().getVersionProtocol())).register();
    }
}
