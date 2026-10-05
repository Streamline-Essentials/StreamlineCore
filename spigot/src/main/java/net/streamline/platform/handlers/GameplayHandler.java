package net.streamline.platform.handlers;

import host.plas.bou.scheduling.TaskManager;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.PlayerInventory;
import singularity.Singularity;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.RandomTeleportArea;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;
import singularity.interfaces.IGameplayHandler;
import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Spigot's {@link IGameplayHandler}, on the Bukkit API. Worlds are named by their Bukkit world
 * name.
 *
 * <p>Calls that answer a question run on the main thread and wait for it when made from
 * elsewhere; actions on a player are scheduled on that player's thread through
 * BukkitOfUtils, which also covers Folia.</p>
 */
public class GameplayHandler implements IGameplayHandler {

    private static final Set<Material> HAZARDS = EnumSet.noneOf(Material.class);

    static {
        for (String name : new String[] {
                "LAVA", "FIRE", "SOUL_FIRE", "CAMPFIRE", "SOUL_CAMPFIRE", "MAGMA_BLOCK", "CACTUS",
                "SWEET_BERRY_BUSH", "WITHER_ROSE", "POWDER_SNOW", "POINTED_DRIPSTONE", "LAVA_CAULDRON",
        }) {
            Material material = Material.matchMaterial(name);
            if (material != null) HAZARDS.add(material);
        }
    }

    /** Players whose flight is kept on; the server clears it on respawn and world change. */
    private final Set<UUID> flightKept = ConcurrentHashMap.newKeySet();

    public GameplayHandler() {
        new BaseRunnable(10, 10) {
            @Override
            public void run() {
                flightKept.forEach(uuid -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) TaskManager.schedule(player, () -> {
                        if (! player.getAllowFlight()) player.setAllowFlight(true);
                    });
                });
            }
        };
    }

    /** Clears per-session state when the player leaves. */
    public void forget(Player player) {
        flightKept.remove(player.getUniqueId());
    }

    @Override
    public Optional<CosmicLocation> getLocation(String uuid) {
        Player player = BasePlugin.getPlayer(uuid);
        if (player == null) return Optional.empty();
        return Optional.of(location(player.getLocation()));
    }

    @Override
    public boolean teleport(String uuid, CosmicLocation location) {
        Player player = BasePlugin.getPlayer(uuid);
        World world = Bukkit.getWorld(location.getWorldName());
        if (player == null || world == null) return false;
        TaskManager.teleport(player, new Location(world, location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch()));
        return true;
    }

    @Override
    public List<String> getWorldNames() {
        List<String> names = new ArrayList<>();
        Bukkit.getWorlds().forEach(world -> names.add(world.getName()));
        return names;
    }

    @Override
    public Optional<CosmicLocation> getWorldSpawn() {
        return callSync(() -> {
            if (Bukkit.getWorlds().isEmpty()) return Optional.<CosmicLocation>empty();
            Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
            World world = spawn.getWorld();
            int y = nearestSafeY(world, spawn.getBlockX(), spawn.getBlockZ(), spawn.getBlockY()).orElse(spawn.getBlockY());
            return Optional.of(location(new Location(world, spawn.getBlockX() + 0.5, y, spawn.getBlockZ() + 0.5,
                    spawn.getYaw(), spawn.getPitch())));
        });
    }

    @Override
    public Optional<CosmicLocation> findSafeLocation(String worldName, int x, int z, int nearY) {
        return callSync(() -> {
            World world = Bukkit.getWorld(worldName);
            if (world == null || ! world.getWorldBorder().isInside(new Location(world, x, 0, z))) {
                return Optional.<CosmicLocation>empty();
            }
            OptionalInt y = nearestSafeY(world, x, z, nearY);
            if (y.isEmpty()) return Optional.<CosmicLocation>empty();
            return Optional.of(location(new Location(world, x + 0.5, y.getAsInt(), z + 0.5)));
        });
    }

    @Override
    public Optional<CosmicLocation> findRandomSafeLocation(String worldName, int minRadius, int maxRadius, int maxAttempts) {
        return callSync(() -> {
            World world = Bukkit.getWorld(worldName);
            if (world == null) return Optional.<CosmicLocation>empty();
            int min = Math.max(0, minRadius);
            int max = Math.max(min + 1, maxRadius);
            boolean overworld = world.getEnvironment() == World.Environment.NORMAL;
            Location center = overworld ? world.getSpawnLocation() : new Location(world, 0, 0, 0);
            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                // Uniform over the ring's area rather than its radius, so landings do not cluster near the center.
                double angle = random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(min * (double) min + random.nextDouble() * (max * (double) max - min * (double) min));
                int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * r);
                int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * r);
                if (! world.getWorldBorder().isInside(new Location(world, x, 0, z))) continue;

                OptionalInt y;
                if (world.getEnvironment() == World.Environment.NETHER) {
                    y = nearestSafeY(world, x, z, 64);
                } else {
                    int surface = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
                    // An empty column (the End's void) reports the bottom of the world.
                    if (surface <= world.getMinHeight() + 1) continue;
                    if (isWater(world.getBiome(x, surface, z))) continue;
                    y = isSafe(world, x, surface, z) ? OptionalInt.of(surface) : OptionalInt.empty();
                }
                if (y.isPresent()) return Optional.of(location(new Location(world, x + 0.5, y.getAsInt(), z + 0.5)));
            }
            return Optional.<CosmicLocation>empty();
        });
    }

    @Override
    public Optional<CosmicLocation> findRandomSafeLocation(RandomTeleportArea area, int maxAttempts) {
        return callSync(() -> {
            World world = Bukkit.getWorld(area.getWorld());
            if (world == null) return Optional.<CosmicLocation>empty();
            int minY = Math.max(area.getMinY(), world.getMinHeight() + 1);
            int maxY = Math.min(area.getMaxY(), topY(world) - 1);
            if (minY > maxY) return Optional.<CosmicLocation>empty();
            boolean ceiling = world.getEnvironment() == World.Environment.NETHER;
            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                int[] column = area.sample(random);
                if (column == null) continue;
                int x = column[0];
                int z = column[1];
                if (! world.getWorldBorder().isInside(new Location(world, x, 0, z))) continue;

                OptionalInt y;
                if (ceiling) {
                    y = nearestSafeY(world, x, z, (minY + maxY) / 2, minY, maxY);
                } else {
                    int surface = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
                    // An empty column (the End's void) reports the bottom of the world.
                    if (surface <= world.getMinHeight() + 1 || surface < minY) continue;
                    // A surface inside the range is the only candidate, so water and lava
                    // surfaces are skipped rather than searched through into the caves below.
                    if (surface <= maxY) y = isSafe(world, x, surface, z) ? OptionalInt.of(surface) : OptionalInt.empty();
                    else y = safeInColumn(world, x, z, maxY, minY);
                }
                if (y.isEmpty()) continue;
                if (area.isAvoided(world.getBiome(x, y.getAsInt(), z).getKey().toString())) continue;
                return Optional.of(location(new Location(world, x + 0.5, y.getAsInt(), z + 0.5)));
            }
            return Optional.<CosmicLocation>empty();
        });
    }

    @Override
    public Optional<CosmicLocation> findSafeLocationInColumn(String worldName, int x, int z, int fromY, int toY) {
        return callSync(() -> {
            World world = Bukkit.getWorld(worldName);
            if (world == null) return Optional.<CosmicLocation>empty();
            OptionalInt y = safeInColumn(world, x, z, fromY, toY);
            if (y.isEmpty()) return Optional.<CosmicLocation>empty();
            return Optional.of(location(new Location(world, x + 0.5, y.getAsInt(), z + 0.5)));
        });
    }

    @Override
    public Optional<CosmicLocation> findTargetedLocation(String uuid, int maxDistance) {
        return callSync(() -> {
            Player player = BasePlugin.getPlayer(uuid);
            if (player == null) return Optional.<CosmicLocation>empty();
            Block block = player.getTargetBlockExact(maxDistance);
            if (block == null) return Optional.<CosmicLocation>empty();
            World world = block.getWorld();
            OptionalInt y = firstSafeAbove(world, block.getX(), block.getZ(), block.getY() + 1);
            if (y.isEmpty()) return Optional.<CosmicLocation>empty();
            Location eye = player.getLocation();
            return Optional.of(location(new Location(world, block.getX() + 0.5, y.getAsInt(), block.getZ() + 0.5,
                    eye.getYaw(), eye.getPitch())));
        });
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean heal(String uuid) {
        Player player = BasePlugin.getPlayer(uuid);
        if (player == null) return false;
        TaskManager.schedule(player, () -> {
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20F);
            player.setFireTicks(0);
        });
        return true;
    }

    @Override
    public boolean setFlight(String uuid, boolean allowed) {
        Player player = BasePlugin.getPlayer(uuid);
        if (player == null) return false;
        if (allowed) flightKept.add(player.getUniqueId());
        else flightKept.remove(player.getUniqueId());
        TaskManager.schedule(player, () -> {
            if (allowed) {
                player.setAllowFlight(true);
            } else if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        });
        return true;
    }

    @Override
    public HatResult wearHeldItem(String uuid) {
        return callSync(() -> {
            Player player = BasePlugin.getPlayer(uuid);
            if (player == null) return HatResult.OFFLINE;
            PlayerInventory inventory = player.getInventory();
            ItemStack hand = inventory.getItemInMainHand();
            ItemStack head = inventory.getHelmet();
            if (hand.getType() == Material.AIR || hand.getAmount() <= 0) return HatResult.EMPTY_HAND;
            boolean hasHead = head != null && head.getType() != Material.AIR;
            if (hasHead && player.getGameMode() != GameMode.CREATIVE && head.containsEnchantment(Enchantment.BINDING_CURSE)) {
                return HatResult.CURSED_HELMET;
            }

            if (hand.getAmount() == 1) {
                inventory.setHelmet(hand.clone());
                inventory.setItemInMainHand(hasHead ? head.clone() : null);
            } else {
                // Only one item is worn; the rest of the stack stays in hand and the old helmet goes to the inventory.
                ItemStack worn = hand.clone();
                worn.setAmount(1);
                hand.setAmount(hand.getAmount() - 1);
                inventory.setItemInMainHand(hand);
                inventory.setHelmet(worn);
                if (hasHead) {
                    inventory.addItem(head.clone()).values()
                            .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
                }
            }
            return HatResult.SUCCESS;
        });
    }

    @Override
    public boolean openDisposal(String uuid, String title, int rows) {
        Player player = BasePlugin.getPlayer(uuid);
        if (player == null) return false;
        int r = Math.max(1, Math.min(6, rows));
        TaskManager.schedule(player, () -> player.openInventory(Bukkit.createInventory(null, 9 * r, coded(title))));
        return true;
    }

    @Override
    public boolean openWorkstation(String uuid, Workstation type) {
        Player player = BasePlugin.getPlayer(uuid);
        if (player == null) return false;
        return callSync(() -> {
            try {
                // A view made by MenuType#create does not check that the block is nearby, so
                // it stays open wherever the player goes. MenuType exists from 1.21.
                player.openInventory(menuType(type).create(player, type.getTitle()));
                return true;
            } catch (LinkageError noMenuTypes) {
                return openWorkstationLegacy(player, type);
            }
        });
    }

    @SuppressWarnings({"UnstableApiUsage", "deprecation"})
    private static MenuType.Typed<?, ?> menuType(Workstation type) {
        switch (type) {
            case CRAFTING: return MenuType.CRAFTING;
            case ANVIL: return MenuType.ANVIL;
            case SMITHING: return MenuType.SMITHING;
            case GRINDSTONE: return MenuType.GRINDSTONE;
            case STONECUTTER: return MenuType.STONECUTTER;
            case CARTOGRAPHY: return MenuType.CARTOGRAPHY_TABLE;
            case LOOM: return MenuType.LOOM;
            default: return MenuType.ENCHANTMENT;
        }
    }

    /**
     * Servers older than 1.21: Bukkit opens crafting and enchanting tables; the others need
     * Paper's methods, and plain Spigot has no way to open them.
     */
    @SuppressWarnings("deprecation")
    private static boolean openWorkstationLegacy(Player player, Workstation type) {
        try {
            switch (type) {
                case CRAFTING: return player.openWorkbench(null, true) != null;
                case ENCHANTING: return player.openEnchanting(null, true) != null;
                case ANVIL: return player.openAnvil(null, true) != null;
                case SMITHING: return player.openSmithingTable(null, true) != null;
                case GRINDSTONE: return player.openGrindstone(null, true) != null;
                case STONECUTTER: return player.openStonecutter(null, true) != null;
                case CARTOGRAPHY: return player.openCartographyTable(null, true) != null;
                case LOOM: return player.openLoom(null, true) != null;
                default: return false;
            }
        } catch (LinkageError notPaper) {
            return false;
        }
    }

    @Override
    public boolean openInventoryOf(String viewerUuid, String targetUuid) {
        Player viewer = BasePlugin.getPlayer(viewerUuid);
        Player target = BasePlugin.getPlayer(targetUuid);
        if (viewer == null || target == null) return false;
        // A player's inventory opened by someone else is live: changes apply to the owner at once.
        TaskManager.schedule(viewer, () -> viewer.openInventory(target.getInventory()));
        return true;
    }

    @Override
    public boolean hasPlayedBefore(String uuid) {
        Player player = BasePlugin.getPlayer(uuid);
        return player != null && player.hasPlayedBefore();
    }

    @Override
    public void refreshDisplayName(String uuid) {
        Player player = BasePlugin.getPlayer(uuid);
        if (player == null) return;
        String nickname = UserUtils.getPlayer(uuid).map(p -> p.getMeta().getNickname()).orElse("");
        TaskManager.schedule(player, () -> {
            if (nickname == null || nickname.isBlank()) {
                player.setDisplayName(player.getName());
                player.setPlayerListName(null);
            } else {
                player.setDisplayName(coded(nickname));
                player.setPlayerListName(coded(nickname));
            }
        });
    }

    @Override
    public boolean isOperator(String uuid) {
        Player player = BasePlugin.getPlayer(uuid);
        return player != null && player.isOp();
    }

    private static boolean isWater(Biome biome) {
        String key = biome.getKey().getKey();
        return key.contains("ocean") || key.contains("river");
    }

    /** True when the feet block and the one above are open and the block below is solid, safe ground. */
    private static boolean isSafe(World world, int x, int y, int z) {
        if (y <= world.getMinHeight() || y + 1 > topY(world)) return false;
        Block ground = world.getBlockAt(x, y - 1, z);
        if (ground.isLiquid() || HAZARDS.contains(ground.getType()) || ! ground.getType().isSolid()) return false;
        return isOpen(world.getBlockAt(x, y, z)) && isOpen(world.getBlockAt(x, y + 1, z));
    }

    private static boolean isOpen(Block block) {
        return block.isPassable() && ! block.isLiquid() && ! HAZARDS.contains(block.getType());
    }

    /** Highest Y a player's head may occupy; in the Nether this stays below the bedrock roof. */
    private static int topY(World world) {
        if (world.getEnvironment() == World.Environment.NETHER) return 126;
        return world.getMaxHeight() - 1;
    }

    private static OptionalInt nearestSafeY(World world, int x, int z, int preferredY) {
        return nearestSafeY(world, x, z, preferredY, world.getMinHeight() + 1, topY(world) - 1);
    }

    /** The safe feet Y in [min, max] closest to {@code preferredY}, searching up and down alternately. */
    private static OptionalInt nearestSafeY(World world, int x, int z, int preferredY, int min, int max) {
        int start = Math.max(min, Math.min(max, preferredY));
        for (int offset = 0; start - offset >= min || start + offset <= max; offset++) {
            int up = start + offset;
            if (up <= max && isSafe(world, x, up, z)) return OptionalInt.of(up);
            int down = start - offset;
            if (offset > 0 && down >= min && isSafe(world, x, down, z)) return OptionalInt.of(down);
        }
        return OptionalInt.empty();
    }

    private static OptionalInt firstSafeAbove(World world, int x, int z, int fromY) {
        for (int y = Math.max(fromY, world.getMinHeight() + 1); y < topY(world); y++) {
            if (isSafe(world, x, y, z)) return OptionalInt.of(y);
        }
        return OptionalInt.empty();
    }

    /** The first safe feet Y from {@code fromY} toward {@code toY}, both inclusive, within the world's height. */
    private static OptionalInt safeInColumn(World world, int x, int z, int fromY, int toY) {
        int min = world.getMinHeight() + 1;
        int max = topY(world) - 1;
        int low = Math.max(min, Math.min(fromY, toY));
        int high = Math.min(max, Math.max(fromY, toY));
        if (low > high) return OptionalInt.empty();
        boolean up = toY >= fromY;
        for (int y = up ? low : high; up ? y <= high : y >= low; y += up ? 1 : -1) {
            if (isSafe(world, x, y, z)) return OptionalInt.of(y);
        }
        return OptionalInt.empty();
    }

    private static String coded(String text) {
        return Messenger.getInstance() != null ? Messenger.getInstance().codedString(text) : text.replace('&', '§');
    }

    private static CosmicLocation location(Location loc) {
        return new CosmicLocation(new CosmicServer(Singularity.getServerName()), new PlayerWorld(loc.getWorld().getName()),
                new WorldPosition(loc.getX(), loc.getY(), loc.getZ()), new PlayerRotation(loc.getYaw(), loc.getPitch()));
    }

    /** Runs the task on the main thread, waiting for it when called from elsewhere. */
    private static <T> T callSync(Supplier<T> task) {
        if (Bukkit.isPrimaryThread()) return task.get();
        try {
            return Bukkit.getScheduler().callSyncMethod(BasePlugin.getInstance(), task::get).get();
        } catch (Exception e) {
            throw new IllegalStateException("Main-thread task failed", e);
        }
    }
}
