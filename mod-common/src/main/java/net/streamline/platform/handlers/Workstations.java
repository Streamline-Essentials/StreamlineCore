package net.streamline.platform.handlers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import singularity.interfaces.IGameplayHandler.Workstation;

/**
 * Vanilla workstation menus opened without their block. Each menu's {@code stillValid} would
 * close it at once because no such block stands at the access position, so every menu here
 * reports itself valid. The access position is still the player's, which is where items left
 * in the input slots are returned and where the enchanting table counts bookshelves.
 */
final class Workstations {

    private Workstations() {}

    static MenuProvider provider(Workstation type, ServerLevel level, BlockPos pos) {
        ContainerLevelAccess access = ContainerLevelAccess.create(level, pos);
        return new SimpleMenuProvider((id, inventory, player) -> menu(type, id, inventory, access), title(type));
    }

    private static AbstractContainerMenu menu(Workstation type, int id, Inventory inventory, ContainerLevelAccess access) {
        switch (type) {
            case CRAFTING:
                return new CraftingMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(Player player) {
                        return true;
                    }
                };
            case ANVIL:
                return new AnvilMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(Player player) {
                        return true;
                    }
                };
            case SMITHING:
                return new SmithingMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(Player player) {
                        return true;
                    }
                };
            case GRINDSTONE:
                return new GrindstoneMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(Player player) {
                        return true;
                    }
                };
            case STONECUTTER:
                return new StonecutterMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(Player player) {
                        return true;
                    }
                };
            case CARTOGRAPHY:
                return new CartographyTableMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(Player player) {
                        return true;
                    }
                };
            case LOOM:
                return new LoomMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(Player player) {
                        return true;
                    }
                };
            default:
                return new EnchantmentMenu(id, inventory, access) {
                    @Override
                    public boolean stillValid(Player player) {
                        return true;
                    }
                };
        }
    }

    /** The vanilla title, translated by the client. */
    private static Component title(Workstation type) {
        switch (type) {
            case CRAFTING: return Component.translatable("container.crafting");
            case ANVIL: return Component.translatable("container.repair");
            case SMITHING: return Component.translatable("container.upgrade");
            case GRINDSTONE: return Component.translatable("container.grindstone_title");
            case STONECUTTER: return Component.translatable("container.stonecutter");
            case CARTOGRAPHY: return Component.translatable("container.cartography_table");
            case LOOM: return Component.translatable("container.loom");
            default: return Component.translatable("container.enchant");
        }
    }
}
