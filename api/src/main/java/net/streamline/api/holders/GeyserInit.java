package net.streamline.api.holders;

import lombok.Getter;
import lombok.Setter;
import singularity.holders.HolderInit;
import singularity.holders.HoldersHolder;

@Getter @Setter
public class GeyserInit extends HolderInit<GeyserHolder> {
    public GeyserInit() {
        super(HoldersHolder.GEYSER_IDENTIFIER, GeyserHolder::new);
    }

    @Override
    public void onLoad() {

    }
}
