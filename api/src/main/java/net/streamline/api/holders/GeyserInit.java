package net.streamline.api.holders;

import lombok.Getter;
import lombok.Setter;
import singularity.holders.HolderInit;
import singularity.holders.HoldersHolder;

/**
 * Initializer that registers a {@link GeyserHolder} under the standard Geyser
 * identifier so that the Singularity dependency-holder framework can locate and
 * manage the Floodgate API integration.
 */
@Getter @Setter
public class GeyserInit extends HolderInit<GeyserHolder> {

    /**
     * Constructs a new {@code GeyserInit} and registers a {@link GeyserHolder}
     * factory under {@link HoldersHolder#GEYSER_IDENTIFIER}.
     */
    public GeyserInit() {
        super(HoldersHolder.GEYSER_IDENTIFIER, GeyserHolder::new);
    }

    /**
     * {@inheritDoc}
     * <p>
     * No additional setup is required after the holder is loaded; this
     * implementation is intentionally empty.
     */
    @Override
    public void onLoad() {

    }
}
