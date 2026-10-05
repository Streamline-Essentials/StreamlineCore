package net.streamline.platform.compat;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;

/**
 * A {@link ChestMenu} that hands every click to {@link #onClick} instead of moving items.
 * The click kind is passed by its constant name ({@code PICKUP}, {@code QUICK_MOVE}, ...), which
 * is the same on every version; only the type carrying it differs ({@code ClickType} up to
 * 1.21.11, {@code ContainerInput} from 26.1).
 */
public abstract class CompatChestMenu extends ChestMenu {

    protected CompatChestMenu(MenuType<?> type, int containerId, Inventory inventory, Container container, int rows) {
        super(type, containerId, inventory, container, rows);
    }

    @Override
    public void clicked(int slot, int button, ContainerInput input, Player player) {
        onClick(slot, button, input.name(), player);
    }

    protected abstract void onClick(int slot, int button, String input, Player player);
}
