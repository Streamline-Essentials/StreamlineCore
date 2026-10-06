package net.streamline.platform.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enderman;

/**
 * Enderman access from 26.3, where the class is {@code Enderman}. Up to 26.2 it is
 * {@code EnderMan}: see {@code mod-common/compat/enderman-upto262}.
 */
public final class CompatEnderman {

    private CompatEnderman() {}

    /** Whether the entity is an enderman holding a block. */
    public static boolean isCarryingBlock(Entity entity) {
        return entity instanceof Enderman && ((Enderman) entity).getCarriedBlock() != null;
    }
}
