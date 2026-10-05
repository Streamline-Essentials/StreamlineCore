package singularity.gui;

import lombok.Getter;
import singularity.data.players.CosmicPlayer;

/**
 * A click on a {@link CosmicGui} slot, handed to the clicked {@link GuiIcon}'s handler.
 */
@Getter
public class GuiClick {
    private final CosmicPlayer viewer;
    private final CosmicGui gui;
    private final int slot;
    private final GuiClickType type;

    public GuiClick(CosmicPlayer viewer, CosmicGui gui, int slot, GuiClickType type) {
        this.viewer = viewer;
        this.gui = gui;
        this.slot = slot;
        this.type = type;
    }

    /** Closes the GUI for the viewer. */
    public void close() {
        GuiManager.close(viewer);
    }

    /** Opens another GUI for the viewer in place of this one. */
    public void open(CosmicGui other) {
        other.open(viewer);
    }

    /** Rebuilds and resends this GUI to everyone viewing it. */
    public void redraw() {
        gui.redraw();
    }
}
