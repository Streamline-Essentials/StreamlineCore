package net.streamline.platform.handlers.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.streamline.platform.compat.CompatChestMenu;
import net.streamline.platform.compat.McCompat;
import singularity.gui.GuiClickType;
import singularity.gui.GuiManager;
import singularity.gui.GuiView;

/**
 * A chest menu showing one {@link GuiView}. No click moves an item: each one is reported to
 * {@link GuiManager} (when its slot is clickable) and the client is then resynchronised, undoing
 * whatever it predicted.
 */
public class CosmicMenu extends CompatChestMenu {
    private static final MenuType<?>[] TYPES = {
            MenuType.GENERIC_9x1, MenuType.GENERIC_9x2, MenuType.GENERIC_9x3,
            MenuType.GENERIC_9x4, MenuType.GENERIC_9x5, MenuType.GENERIC_9x6,
    };

    private final ModGuiHandler handler;
    private final ServerPlayer viewer;
    private final SimpleContainer display;
    private GuiView view;

    private CosmicMenu(int containerId, Inventory inventory, SimpleContainer items, ServerPlayer viewer,
                       GuiView view, ModGuiHandler handler) {
        super(TYPES[view.getRows() - 1], containerId, inventory, items, view.getRows());
        this.handler = handler;
        this.viewer = viewer;
        this.display = items;
        this.view = view;
        fill(view);
    }

    ServerPlayer getViewer() {
        return viewer;
    }

    GuiView getView() {
        return view;
    }

    static CosmicMenu create(int containerId, Inventory inventory, ServerPlayer viewer, GuiView view, ModGuiHandler handler) {
        return new CosmicMenu(containerId, inventory, new SimpleContainer(view.getSize()), viewer, view, handler);
    }

    /** Swaps in {@code view}'s items. The view must have this menu's size. */
    void apply(GuiView view) {
        this.view = view;
        fill(view);
        broadcastChanges();
    }

    private void fill(GuiView view) {
        for (int slot = 0; slot < display.getContainerSize(); slot++) {
            ItemStack stack = McCompat.guiItem(view.getItems().get(slot));
            display.setItem(slot, stack);
        }
    }

    @Override
    protected void onClick(int slot, int button, String input, Player player) {
        if (slot >= 0 && slot < view.getSize() && view.isClickable(slot)) {
            GuiManager.handleClick(viewer.getStringUUID(), view.getId(), slot, clickType(input, button));
        }
        sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (handler.release(this)) GuiManager.handleClosed(viewer.getStringUUID(), view.getId());
    }

    /** Maps a vanilla click (by its constant name and button) onto a {@link GuiClickType}. */
    static GuiClickType clickType(String input, int button) {
        switch (input) {
            case "PICKUP":
                return button == 1 ? GuiClickType.RIGHT : GuiClickType.LEFT;
            case "QUICK_MOVE":
                return button == 1 ? GuiClickType.SHIFT_RIGHT : GuiClickType.SHIFT_LEFT;
            case "SWAP":
                return button >= 0 && button < 9 ? GuiClickType.NUMBER_KEY : GuiClickType.OTHER;
            case "CLONE":
                return GuiClickType.MIDDLE;
            case "THROW":
                return button == 1 ? GuiClickType.CONTROL_DROP : GuiClickType.DROP;
            case "PICKUP_ALL":
                return GuiClickType.DOUBLE_CLICK;
            default:
                return GuiClickType.OTHER;
        }
    }
}
