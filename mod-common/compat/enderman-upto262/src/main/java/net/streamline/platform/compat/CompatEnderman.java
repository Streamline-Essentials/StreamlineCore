package net.streamline.platform.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;

/**
 * Enderman access up to 26.2, where the class is {@code EnderMan}. From 26.3 it is
 * {@code Enderman}: see {@code mod-common/compat/enderman-263}.
 */
public final class CompatEnderman {

    private CompatEnderman() {}

    /** Whether the entity is an enderman holding a block. */
    public static boolean isCarryingBlock(Entity entity) {
        return entity instanceof EnderMan && ((EnderMan) entity).getCarriedBlock() != null;
    }
}
