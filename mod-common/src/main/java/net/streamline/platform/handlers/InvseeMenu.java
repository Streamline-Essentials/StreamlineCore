package net.streamline.platform.handlers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * A vanilla 9x5 chest screen showing another player's live inventory, so it works for
 * unmodded clients. Layout:
 * <pre>
 * rows 0-2  main inventory (slots 9-35)
 * row  3    hotbar         (slots 0-8)
 * row  4    armor feet..head (36-39), offhand (40), then four locked, always-empty cells
 * </pre>
 */
public final class InvseeMenu extends ChestMenu {

    private static final int ROWS = 5;
    private static final int SIZE = 9 * ROWS;

    public InvseeMenu(int containerId, Inventory viewerInventory, ServerPlayer target) {
        super(MenuType.GENERIC_9x5, containerId, viewerInventory, new TargetInventory(target), ROWS);
    }

    /** Delegates to the target's {@link Inventory}, remapping display cells to inventory slots. */
    private static final class TargetInventory implements Container {

        private static final int LOCKED = -1;

        private final ServerPlayer target;
        private final Inventory inv;

        TargetInventory(ServerPlayer target) {
            this.target = target;
            this.inv = target.getInventory();
        }

        private static int map(int cell) {
            if (cell < 27) return cell + 9;
            if (cell < 36) return cell - 27;
            if (cell < 41) return cell;
            return LOCKED;
        }

        @Override
        public int getContainerSize() {
            return SIZE;
        }

        @Override
        public boolean isEmpty() {
            return inv.isEmpty();
        }

        @Override
        public ItemStack getItem(int cell) {
            int slot = map(cell);
            return slot == LOCKED ? ItemStack.EMPTY : inv.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int cell, int count) {
            int slot = map(cell);
            return slot == LOCKED ? ItemStack.EMPTY : inv.removeItem(slot, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int cell) {
            int slot = map(cell);
            return slot == LOCKED ? ItemStack.EMPTY : inv.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int cell, ItemStack stack) {
            int slot = map(cell);
            if (slot != LOCKED) inv.setItem(slot, stack);
        }

        @Override
        public boolean canPlaceItem(int cell, ItemStack stack) {
            int slot = map(cell);
            return slot != LOCKED && inv.canPlaceItem(slot, stack);
        }

        @Override
        public void setChanged() {
            inv.setChanged();
        }

        /**
         * {@link Inventory#stillValid} checks distance to its owner, which would close the screen
         * immediately for a remote viewer; this stays open while the target is online.
         */
        @Override
        public boolean stillValid(Player viewer) {
            return ! target.isRemoved() && ! target.hasDisconnected() && viewer.isAlive();
        }

        @Override
        public void clearContent() {
            // Never clear another player's inventory through the viewer.
        }
    }
}
