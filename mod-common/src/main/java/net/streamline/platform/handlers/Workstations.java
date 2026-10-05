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
                return new Crafting(id, inventory, access);
            case ANVIL:
                return new Anvil(id, inventory, access);
            case SMITHING:
                return new Smithing(id, inventory, access);
            case GRINDSTONE:
                return new Grindstone(id, inventory, access);
            case STONECUTTER:
                return new Stonecutter(id, inventory, access);
            case CARTOGRAPHY:
                return new Cartography(id, inventory, access);
            case LOOM:
                return new Loom(id, inventory, access);
            default:
                return new Enchanting(id, inventory, access);
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

    private static final class Crafting extends CraftingMenu {
        Crafting(int id, Inventory inventory, ContainerLevelAccess access) {
            super(id, inventory, access);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class Anvil extends AnvilMenu {
        Anvil(int id, Inventory inventory, ContainerLevelAccess access) {
            super(id, inventory, access);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class Smithing extends SmithingMenu {
        Smithing(int id, Inventory inventory, ContainerLevelAccess access) {
            super(id, inventory, access);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class Grindstone extends GrindstoneMenu {
        Grindstone(int id, Inventory inventory, ContainerLevelAccess access) {
            super(id, inventory, access);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class Stonecutter extends StonecutterMenu {
        Stonecutter(int id, Inventory inventory, ContainerLevelAccess access) {
            super(id, inventory, access);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class Cartography extends CartographyTableMenu {
        Cartography(int id, Inventory inventory, ContainerLevelAccess access) {
            super(id, inventory, access);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class Loom extends LoomMenu {
        Loom(int id, Inventory inventory, ContainerLevelAccess access) {
            super(id, inventory, access);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class Enchanting extends EnchantmentMenu {
        Enchanting(int id, Inventory inventory, ContainerLevelAccess access) {
            super(id, inventory, access);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
