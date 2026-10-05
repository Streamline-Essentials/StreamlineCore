package net.streamline.platform.handlers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;
import net.streamline.platform.compat.McCompat;
import singularity.Singularity;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;
import singularity.interfaces.IGameplayHandler;
import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The mod loaders' {@link IGameplayHandler}, written against Mojang-mapped Minecraft only.
 * Each loader subclasses it to refresh player names through its own hooks.
 *
 * <p>Everything that reads or changes the world runs on the server thread: calls from other
 * threads are handed to it, and those that return a value wait for the answer.</p>
 */
public abstract class GameplayHandler implements IGameplayHandler {

    private static final MenuType<?>[] CHEST_MENUS = {
            MenuType.GENERIC_9x1, MenuType.GENERIC_9x2, MenuType.GENERIC_9x3,
            MenuType.GENERIC_9x4, MenuType.GENERIC_9x5, MenuType.GENERIC_9x6,
    };

    /** Players whose flight is kept on; vanilla clears it on respawn and dimension change. */
    private final Set<UUID> flightKept = ConcurrentHashMap.newKeySet();

    /** Nicknames shown in chat and the player list, for loaders that can display them. */
    private static final Map<UUID, Component> DISPLAY_NAMES = new ConcurrentHashMap<>();

    protected GameplayHandler() {
        new BaseRunnable(10, 10) {
            @Override
            public void run() {
                if (flightKept.isEmpty()) return;
                runOnServer(() -> flightKept.forEach(uuid -> {
                    ServerPlayer player = player(uuid.toString());
                    if (player != null && ! player.getAbilities().mayfly) applyFlight(player, true);
                }));
            }
        };
    }

    /**
     * The nickname to show for the player, if they have one. Loader listeners call this from
     * their name-formatting hooks.
     */
    public static Optional<Component> displayName(UUID uuid) {
        return Optional.ofNullable(DISPLAY_NAMES.get(uuid));
    }

    /** Re-renders the player's name everywhere the loader allows. */
    protected abstract void refreshNames(ServerPlayer player);

    /** Clears per-session state when the player leaves. */
    public void forget(ServerPlayer player) {
        flightKept.remove(player.getUUID());
        DISPLAY_NAMES.remove(player.getUUID());
    }

    @Override
    public Optional<CosmicLocation> getLocation(String uuid) {
        return callOnServer(() -> {
            ServerPlayer player = player(uuid);
            if (player == null) return Optional.<CosmicLocation>empty();
            return Optional.of(location((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot()));
        }, Optional.empty());
    }

    @Override
    public boolean teleport(String uuid, CosmicLocation location) {
        return callOnServer(() -> {
            ServerPlayer player = player(uuid);
            ServerLevel level = level(location.getWorldName());
            if (player == null || level == null) return false;
            player.stopRiding();
            player.resetFallDistance();
            McCompat.teleport(player, level, location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch());
            return true;
        }, false);
    }

    @Override
    public List<String> getWorldNames() {
        return callOnServer(() -> {
            List<String> names = new ArrayList<>();
            MinecraftServer server = BasePlugin.getServer();
            if (server != null) server.getAllLevels().forEach(level -> names.add(McCompat.dimensionId(level)));
            return names;
        }, new ArrayList<>());
    }

    @Override
    public Optional<CosmicLocation> getWorldSpawn() {
        return callOnServer(() -> {
            MinecraftServer server = BasePlugin.getServer();
            if (server == null) return Optional.<CosmicLocation>empty();
            McCompat.SpawnPoint spawn = McCompat.worldSpawn(server);
            BlockPos pos = spawn.pos;
            int y = SafeSpots.nearestSafeY(spawn.level, pos.getX(), pos.getZ(), pos.getY()).orElse(pos.getY());
            return Optional.of(location(spawn.level, pos.getX() + 0.5, y, pos.getZ() + 0.5, spawn.yaw, spawn.pitch));
        }, Optional.empty());
    }

    @Override
    public Optional<CosmicLocation> findSafeLocation(String world, int x, int z, int nearY) {
        return callOnServer(() -> {
            ServerLevel level = level(world);
            if (level == null || ! level.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) {
                return Optional.<CosmicLocation>empty();
            }
            OptionalInt y = SafeSpots.nearestSafeY(level, x, z, nearY);
            if (y.isEmpty()) return Optional.<CosmicLocation>empty();
            return Optional.of(location(level, x + 0.5, y.getAsInt(), z + 0.5, 0F, 0F));
        }, Optional.empty());
    }

    @Override
    public Optional<CosmicLocation> findRandomSafeLocation(String world, int minRadius, int maxRadius, int maxAttempts) {
        return callOnServer(() -> {
            ServerLevel level = level(world);
            if (level == null) return Optional.<CosmicLocation>empty();
            return SafeSpots.random(level, level.getRandom(), minRadius, maxRadius, maxAttempts)
                    .map(pos -> location(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F));
        }, Optional.empty());
    }

    @Override
    public Optional<CosmicLocation> findTargetedLocation(String uuid, int maxDistance) {
        return callOnServer(() -> {
            ServerPlayer player = player(uuid);
            if (player == null) return Optional.<CosmicLocation>empty();
            HitResult hit = player.pick(maxDistance, 1.0F, false);
            if (hit.getType() != HitResult.Type.BLOCK || ! (hit instanceof BlockHitResult)) {
                return Optional.<CosmicLocation>empty();
            }
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            ServerLevel level = (ServerLevel) player.level();
            OptionalInt y = SafeSpots.firstSafeAbove(level, pos.getX(), pos.getZ(), pos.getY() + 1);
            if (y.isEmpty()) return Optional.<CosmicLocation>empty();
            return Optional.of(location(level, pos.getX() + 0.5, y.getAsInt(), pos.getZ() + 0.5,
                    player.getYRot(), player.getXRot()));
        }, Optional.empty());
    }

    @Override
    public boolean heal(String uuid) {
        return callOnServer(() -> {
            ServerPlayer player = player(uuid);
            if (player == null) return false;
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);
            player.clearFire();
            return true;
        }, false);
    }

    @Override
    public boolean setFlight(String uuid, boolean allowed) {
        return callOnServer(() -> {
            ServerPlayer player = player(uuid);
            if (player == null) return false;
            if (allowed) flightKept.add(player.getUUID());
            else flightKept.remove(player.getUUID());
            applyFlight(player, allowed);
            return true;
        }, false);
    }

    /**
     * Writes {@code Abilities.mayfly} directly: the abilities packet is all an unmodded client
     * receives, so this is the only flag that lets it fly.
     */
    @SuppressWarnings("deprecation")
    private static void applyFlight(ServerPlayer player, boolean allowed) {
        if (allowed) {
            player.getAbilities().mayfly = true;
        } else if (! player.isCreative() && ! player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
    }

    @Override
    public HatResult wearHeldItem(String uuid) {
        return callOnServer(() -> {
            ServerPlayer player = player(uuid);
            if (player == null) return HatResult.OFFLINE;
            ItemStack hand = player.getMainHandItem();
            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (hand.isEmpty()) return HatResult.EMPTY_HAND;
            if (! head.isEmpty() && ! player.isCreative() && McCompat.hasBindingCurse(head)) return HatResult.CURSED_HELMET;

            if (hand.getCount() == 1) {
                player.setItemSlot(EquipmentSlot.HEAD, hand.copy());
                player.setItemSlot(EquipmentSlot.MAINHAND, head.copy());
            } else {
                // Only one item is worn; the rest of the stack stays in hand and the old helmet goes to the inventory.
                player.setItemSlot(EquipmentSlot.HEAD, hand.split(1));
                if (! head.isEmpty()) giveOrDrop(player, head.copy());
            }
            return HatResult.SUCCESS;
        }, HatResult.OFFLINE);
    }

    @Override
    public boolean openDisposal(String uuid, String title, int rows) {
        int r = Math.max(1, Math.min(6, rows));
        return callOnServer(() -> {
            ServerPlayer player = player(uuid);
            if (player == null) return false;
            // A chest backed by a throwaway container: whatever is inside is gone once it closes.
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new ChestMenu(CHEST_MENUS[r - 1], id, inventory, new SimpleContainer(9 * r), r),
                    Component.literal(coded(title))));
            return true;
        }, false);
    }

    @Override
    public boolean openInventoryOf(String viewerUuid, String targetUuid) {
        return callOnServer(() -> {
            ServerPlayer viewer = player(viewerUuid);
            ServerPlayer target = player(targetUuid);
            if (viewer == null || target == null) return false;
            viewer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new InvseeMenu(id, inventory, target),
                    Component.literal(target.getName().getString() + "'s inventory")));
            return true;
        }, false);
    }

    @Override
    public boolean hasPlayedBefore(String uuid) {
        return callOnServer(() -> {
            ServerPlayer player = player(uuid);
            return player != null && player.getStats().getValue(Stats.CUSTOM.get(Stats.LEAVE_GAME)) > 0;
        }, false);
    }

    @Override
    public void refreshDisplayName(String uuid) {
        runOnServer(() -> {
            ServerPlayer player = player(uuid);
            if (player == null) return;
            String nickname = UserUtils.getPlayer(uuid).map(p -> p.getMeta().getNickname()).orElse("");
            if (nickname == null || nickname.isBlank()) DISPLAY_NAMES.remove(player.getUUID());
            else DISPLAY_NAMES.put(player.getUUID(), Component.literal(coded(nickname)));
            refreshNames(player);
        });
    }

    @Override
    public boolean isOperator(String uuid) {
        return callOnServer(() -> {
            ServerPlayer player = player(uuid);
            return player != null && McCompat.isOperator(player);
        }, false);
    }

    /** Puts the stack into the player's inventory, dropping whatever does not fit at their feet. */
    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (player.getInventory().add(stack) || stack.isEmpty()) return;
        player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack));
    }

    private static String coded(String text) {
        return Messenger.getInstance() != null ? Messenger.getInstance().codedString(text) : text.replace('&', '§');
    }

    private static ServerPlayer player(String uuid) {
        return BasePlugin.getPlayer(uuid);
    }

    /** The loaded level with the dimension id, or {@code null} if there is none. */
    protected static ServerLevel level(String dimensionId) {
        MinecraftServer server = BasePlugin.getServer();
        if (server == null || dimensionId == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            if (McCompat.dimensionId(level).equals(dimensionId)) return level;
        }
        return null;
    }

    /** Where the player is now, with their dimension id as the world. */
    public static CosmicLocation locationOf(ServerPlayer player) {
        return location((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    protected static CosmicLocation location(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        return new CosmicLocation(new CosmicServer(Singularity.getServerName()), new PlayerWorld(McCompat.dimensionId(level)),
                new WorldPosition(x, y, z), new PlayerRotation(yaw, pitch));
    }

    private static <T> T callOnServer(Supplier<T> task, T ifStopped) {
        MinecraftServer server = BasePlugin.getServer();
        if (server == null) return ifStopped;
        if (server.isSameThread()) return task.get();
        return server.submit(task).join();
    }

    private static void runOnServer(Runnable task) {
        MinecraftServer server = BasePlugin.getServer();
        if (server == null) return;
        if (server.isSameThread()) task.run();
        else server.execute(task);
    }
}
