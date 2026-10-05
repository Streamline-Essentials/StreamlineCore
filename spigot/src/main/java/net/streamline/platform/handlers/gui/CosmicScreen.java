package net.streamline.platform.handlers.gui;

import host.plas.bou.gui.GuiType;
import host.plas.bou.gui.InventorySheet;
import host.plas.bou.gui.screens.ScreenInstance;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import singularity.gui.GuiClickType;
import singularity.gui.GuiManager;
import singularity.gui.GuiView;

/**
 * A BukkitOfUtils screen showing one {@link GuiView}. Every click is cancelled, so nothing can be
 * taken from or put into it; clicks on slots the view marks clickable are reported to
 * {@link GuiManager}.
 */
@Getter
public class CosmicScreen extends ScreenInstance {
    private final SpigotGuiHandler handler;
    private GuiView view;

    public CosmicScreen(Player player, GuiView view, SpigotGuiHandler handler) {
        super(player, typeOf(view), InventorySheet.empty(view.getSize()), true);
        this.handler = handler;
        this.view = view;
    }

    private static GuiType typeOf(GuiView view) {
        return new GuiType() {
            @Override
            public String name() {
                return "streamline-gui";
            }

            @Override
            public String toString() {
                return name();
            }

            @Override
            public String getTitle() {
                return view.getTitle();
            }
        };
    }

    /** Writes {@code view}'s items into the open inventory. The view must fit this screen. */
    public void apply(GuiView view) {
        this.view = view;
        Inventory inventory = getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, BukkitItems.toStack(view.getItems().get(slot)));
        }
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        apply(view);
    }

    @Override
    public boolean onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot >= 0 && slot < view.getSize() && view.isClickable(slot)) {
            GuiManager.handleClick(getPlayer().getUniqueId().toString(), view.getId(), slot,
                    GuiClickType.fromName(event.getClick().name()));
        }
        return false;
    }

    @Override
    public boolean onDrag(InventoryDragEvent event) {
        return true;
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        super.onClose(event);
        if (handler.release(this)) {
            GuiManager.handleClosed(getPlayer().getUniqueId().toString(), view.getId());
        }
    }
}
