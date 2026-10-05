package net.streamline.platform.listeners;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.streamline.platform.compat.McCompat;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.events.player.gameplay.PlayerBrokeBlockEvent;
import singularity.events.player.gameplay.PlayerCaughtFishEvent;
import singularity.events.player.gameplay.PlayerKilledEntityEvent;
import singularity.events.player.gameplay.PlayerPlacedBlockEvent;
import singularity.gui.CosmicItem;
import singularity.objects.world.CosmicBlock;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fires the cross-platform {@code singularity.events.player.gameplay} events for the mod
 * loaders. Each loader's listener calls in here only for actions that went through, after every
 * other mod had the chance to cancel them.
 */
public final class GameplayEvents {

    private GameplayEvents() {}

    public static void onBlockBroken(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        CosmicPlayer cosmic = cosmic(player);
        if (cosmic == null || ! (level instanceof ServerLevel)) return;

        IntegerProperty age = ageProperty(state);
        boolean ageable = age != null;
        boolean mature = true;
        if (ageable) {
            int max = 0;
            for (Integer value : age.getPossibleValues()) max = Math.max(max, value);
            mature = state.getValue(age) >= max;
        }

        fire(new PlayerBrokeBlockEvent(cosmic, gameMode(player), block((ServerLevel) level, pos, state), ageable, mature));
    }

    public static void onBlockPlaced(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        CosmicPlayer cosmic = cosmic(player);
        if (cosmic == null || ! (level instanceof ServerLevel)) return;

        fire(new PlayerPlacedBlockEvent(cosmic, gameMode(player), block((ServerLevel) level, pos, state)));
    }

    public static void onEntityKilled(ServerPlayer killer, Entity entity, List<ItemStack> drops) {
        CosmicPlayer cosmic = cosmic(killer);
        if (cosmic == null) return;

        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        fire(new PlayerKilledEntityEvent(cosmic, gameMode(killer), type, items(drops)));
    }

    public static void onFishCaught(ServerPlayer player, List<ItemStack> caught) {
        CosmicPlayer cosmic = cosmic(player);
        if (cosmic == null) return;

        fire(new PlayerCaughtFishEvent(cosmic, gameMode(player), items(caught)));
    }

    /** The stacks carried by dropped item entities. */
    public static List<ItemStack> stacksOf(java.util.Collection<ItemEntity> entities) {
        List<ItemStack> stacks = new ArrayList<>();
        if (entities == null) return stacks;
        for (ItemEntity entity : entities) stacks.add(entity.getItem());
        return stacks;
    }

    /** The block's {@code age} property, which every growing plant uses, or {@code null}. */
    private static IntegerProperty ageProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty && property.getName().equals("age")) return (IntegerProperty) property;
        }
        return null;
    }

    private static List<CosmicItem> items(List<ItemStack> stacks) {
        List<CosmicItem> items = new ArrayList<>();
        if (stacks == null) return items;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            items.add(CosmicItem.of(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).setAmount(stack.getCount()));
        }
        return items;
    }

    private static CosmicBlock block(ServerLevel level, BlockPos pos, BlockState state) {
        return new CosmicBlock(new PlayerWorld(McCompat.dimensionId(level)),
                new WorldPosition(pos.getX(), pos.getY(), pos.getZ()),
                BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    private static String gameMode(ServerPlayer player) {
        return player.gameMode.getGameModeForPlayer().getName();
    }

    private static CosmicPlayer cosmic(ServerPlayer player) {
        if (player == null) return null;
        return UserUtils.getOrCreatePlayer(player.getStringUUID()).orElse(null);
    }

    private static void fire(singularity.events.CosmicEvent event) {
        try {
            event.fire();
        } catch (Throwable t) {
            MessageUtils.logWarning("A listener for " + event.getClass().getSimpleName() + " failed", t);
        }
    }
}
