package singularity.interfaces;

import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.RandomTeleportArea;

import java.util.List;
import java.util.Optional;

/**
 * In-game actions on players and worlds that backend platforms (Spigot and the mod loaders)
 * implement, so modules can offer essentials-style features without touching any
 * platform's classes. Proxies have no worlds and provide no handler; see
 * {@link singularity.Singularity#gameplay()}.
 *
 * <p>Players are identified by UUID string and worlds by the platform's own name for them:
 * the world name on Spigot, the dimension id (for example {@code minecraft:the_nether}) on
 * the mod loaders. Locations returned here carry that name as their world.</p>
 *
 * <p>Implementations may be called from any thread and move work onto the server thread
 * themselves. Methods that answer a question wait for that work to finish, so call them from
 * command handlers or the server thread rather than from code holding locks the server thread
 * needs.</p>
 */
public interface IGameplayHandler {

    /** Outcome of {@link #wearHeldItem(String)}. */
    enum HatResult {
        /** The held item now sits in the head slot. */
        SUCCESS,
        /** The player's main hand is empty. */
        EMPTY_HAND,
        /** The current helmet carries Curse of Binding and cannot be swapped out. */
        CURSED_HELMET,
        /** The player is not online on this server. */
        OFFLINE,
    }

    /** Blocks whose screen {@link #openWorkstation(String, Workstation)} can open. */
    enum Workstation {
        CRAFTING("Crafting"),
        ANVIL("Repair & Name"),
        SMITHING("Upgrade Gear"),
        GRINDSTONE("Repair & Disenchant"),
        STONECUTTER("Stonecutter"),
        CARTOGRAPHY("Cartography Table"),
        LOOM("Loom"),
        ENCHANTING("Enchant"),
        ;

        /** The screen's title where the platform needs one spelled out, matching vanilla's English title. */
        private final String title;

        Workstation(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    /**
     * The player's current position, world and facing.
     *
     * @param uuid the player's UUID
     * @return the location, or empty if the player is not online on this server
     */
    Optional<CosmicLocation> getLocation(String uuid);

    /**
     * Moves the player to the location, changing world if needed. The location's server is
     * not consulted; this always teleports within this server.
     *
     * @param uuid     the player's UUID
     * @param location where to send them
     * @return {@code false} if the player is offline or the location's world does not exist
     */
    boolean teleport(String uuid, CosmicLocation location);

    /**
     * @return the names of every loaded world, in the platform's naming
     */
    List<String> getWorldNames();

    /**
     * The world spawn players are sent to by default, at a safe height where one exists.
     *
     * @return the spawn, or empty if the server is not running
     */
    Optional<CosmicLocation> getWorldSpawn();

    /**
     * The safe standing spot in column (x, z) of the world closest in height to {@code nearY}.
     * A spot is safe when the player has solid ground below, two open blocks to stand in, and
     * no lava, fire or similar hazard around their feet and head.
     *
     * @param world the world name
     * @param x     block X
     * @param z     block Z
     * @param nearY preferred block Y
     * @return the spot, or empty if the column has none or lies outside the world border
     */
    Optional<CosmicLocation> findSafeLocation(String world, int x, int z, int nearY);

    /**
     * A random safe standing spot whose horizontal distance from the world's spawn (from 0,0 in
     * worlds without one) lies between the radii. Oceans and rivers are skipped and the world
     * border is respected.
     *
     * @param world       the world name
     * @param minRadius   minimum distance from the center
     * @param maxRadius   maximum distance from the center
     * @param maxAttempts candidate columns to try; each may generate a chunk
     * @return the spot, or empty if no attempt found one
     */
    Optional<CosmicLocation> findRandomSafeLocation(String world, int minRadius, int maxRadius, int maxAttempts);

    /**
     * A random safe standing spot inside the area, at a height within its Y range. In worlds
     * without a ceiling the spot is on the surface when the surface lies in that range, and
     * otherwise the highest safe spot below the range's top; in worlds with a ceiling (the
     * Nether) it is the safe spot nearest the middle of the range. The world border is
     * respected.
     *
     * @param area        where the spot may be
     * @param maxAttempts candidate columns to try; each may generate a chunk
     * @return the spot, or empty if the world does not exist or no attempt found one
     */
    Optional<CosmicLocation> findRandomSafeLocation(RandomTeleportArea area, int maxAttempts);

    /**
     * The first safe standing spot in column (x, z), checking each feet Y from {@code fromY}
     * toward {@code toY}, both inclusive. Either bound may lie outside the world; the search
     * stops at the world's floor and ceiling. The spot is centered on its block.
     *
     * @param world the world name
     * @param x     block X
     * @param z     block Z
     * @param fromY first feet Y to check
     * @param toY   last feet Y to check; below {@code fromY} to search downward
     * @return the spot, or empty if the world does not exist or the range holds none
     */
    Optional<CosmicLocation> findSafeLocationInColumn(String world, int x, int z, int fromY, int toY);

    /**
     * The first safe standing spot on top of the block the player is looking at.
     *
     * @param uuid        the player's UUID
     * @param maxDistance how far the player can see, in blocks
     * @return the spot, or empty if no block is in sight or there is no room on it
     */
    Optional<CosmicLocation> findTargetedLocation(String uuid, int maxDistance);

    /**
     * Restores the player's health, hunger and saturation to full and puts out any fire.
     *
     * @param uuid the player's UUID
     * @return {@code false} if the player is not online on this server
     */
    boolean heal(String uuid);

    /**
     * Allows or forbids creative-style flight. While allowed, the platform keeps it allowed
     * across respawns, world changes and game mode changes until the player logs out; the
     * caller re-applies it at the next login if it should persist. Forbidding flight leaves
     * creative and spectator players able to fly.
     *
     * @param uuid    the player's UUID
     * @param allowed whether the player may fly
     * @return {@code false} if the player is not online on this server
     */
    boolean setFlight(String uuid, boolean allowed);

    /**
     * Moves one item from the player's main hand onto their head. The old helmet, if any,
     * goes back into their inventory, or is dropped when the inventory is full.
     *
     * @param uuid the player's UUID
     * @return what happened
     */
    HatResult wearHeldItem(String uuid);

    /**
     * Opens an empty chest screen whose contents are discarded when it closes.
     *
     * @param uuid  the player's UUID
     * @param title the screen title, with {@code &} color codes
     * @param rows  rows of nine slots, 1 to 6
     * @return {@code false} if the player is not online on this server
     */
    boolean openDisposal(String uuid, String title, int rows);

    /**
     * Opens a workstation's screen as if the player had used that block, without one being
     * there. The screen stays open wherever the player moves.
     *
     * @param uuid the player's UUID
     * @param type the workstation
     * @return {@code false} if the player is not online on this server, or the server does not
     *         support opening that workstation remotely
     */
    boolean openWorkstation(String uuid, Workstation type);

    /**
     * Shows the target's live inventory (main inventory, hotbar, armor and offhand) to the
     * viewer, who can move items in and out of it.
     *
     * @param viewerUuid the viewer's UUID
     * @param targetUuid the UUID of the player whose inventory is shown
     * @return {@code false} if either player is not online on this server
     */
    boolean openInventoryOf(String viewerUuid, String targetUuid);

    /**
     * Whether the player has ever left this server before, according to the game's own
     * records. Players who joined before any Streamline module was installed count.
     *
     * @param uuid the player's UUID
     * @return {@code false} on the player's very first session
     */
    boolean hasPlayedBefore(String uuid);

    /**
     * Re-reads the player's nickname from their Streamline metadata and shows it in chat and
     * the player list, where the platform supports that.
     *
     * @param uuid the player's UUID
     */
    void refreshDisplayName(String uuid);

    /**
     * Whether the player is a server operator (level 2 or higher on the mod loaders).
     *
     * @param uuid the player's UUID
     * @return {@code false} for offline players
     */
    boolean isOperator(String uuid);
}
