package host.plas.data;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DifferedSetup {
    private boolean enabled;

    private DifferedType type;

    private PlainHook plain;
    private EmbedHook embed;

    public DifferedSetup(boolean enabled, DifferedType type, PlainHook plain, EmbedHook embed) {
        this.enabled = enabled;
        this.type = type;
        this.plain = plain;
        this.embed = embed;
    }
}