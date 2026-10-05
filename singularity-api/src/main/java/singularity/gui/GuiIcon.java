package singularity.gui;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import singularity.Singularity;
import singularity.utils.UserUtils;

import java.util.function.Consumer;

/**
 * One slot's content in a {@link CosmicGui}: the item shown and what clicking it does.
 *
 * <p>The click handler always runs on the server that built the GUI. When a proxy sends a GUI
 * to a backend, the backend only renders it and reports clicks back, so handlers may freely use
 * whatever the building server has in memory.</p>
 */
@Getter @Setter
@Accessors(chain = true)
public class GuiIcon {
    private CosmicItem item;
    private Consumer<GuiClick> onClick;

    public GuiIcon(CosmicItem item, Consumer<GuiClick> onClick) {
        this.item = item == null ? CosmicItem.air() : item;
        this.onClick = onClick;
    }

    public GuiIcon(CosmicItem item) {
        this(item, null);
    }

    public static GuiIcon of(CosmicItem item) {
        return new GuiIcon(item);
    }

    public static GuiIcon of(CosmicItem item, Consumer<GuiClick> onClick) {
        return new GuiIcon(item, onClick);
    }

    /** An icon that closes the GUI when clicked. */
    public static GuiIcon close(CosmicItem item) {
        return new GuiIcon(item, GuiClick::close);
    }

    /**
     * An icon that makes the viewer run {@code command} (without the leading slash) on the server
     * that built the GUI.
     */
    public static GuiIcon playerCommand(CosmicItem item, String command, boolean closeAfter) {
        return new GuiIcon(item, click -> {
            if (closeAfter) click.close();
            Singularity.getInstance().getUserManager().runAs(click.getViewer(), false, command);
        });
    }

    /**
     * An icon that runs {@code command} (without the leading slash) as the console of the server
     * that built the GUI. {@code %player%} and {@code %uuid%} are replaced with the viewer's name
     * and UUID.
     */
    public static GuiIcon consoleCommand(CosmicItem item, String command, boolean closeAfter) {
        return new GuiIcon(item, click -> {
            if (closeAfter) click.close();
            String resolved = command
                    .replace("%player%", click.getViewer().getCurrentName())
                    .replace("%uuid%", click.getViewer().getUuid());
            Singularity.getInstance().getUserManager().runAs(UserUtils.getConsole(), true, resolved);
        });
    }

    public boolean isClickable() {
        return onClick != null;
    }
}
