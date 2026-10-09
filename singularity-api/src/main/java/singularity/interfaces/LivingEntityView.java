package singularity.interfaces;

/**
 * A read-only look at one living entity (a mob, an animal, an armor stand or a player) during
 * {@link IGameplayHandler#damageLivingEntities}. A view is only valid inside the call it is
 * handed to, which runs on the thread that owns the entity; do not keep it.
 *
 * <p>Ids are namespaced and lowercase, as in {@code minecraft:zombie} and
 * {@code minecraft:stonecutter}. Worlds are named as {@link IGameplayHandler} names them: the
 * world name on Spigot, the dimension id on the mod loaders.</p>
 */
public interface LivingEntityView {

    /** @return the entity's UUID */
    String getUuid();

    /** @return the entity type id, such as {@code minecraft:zombie} or {@code minecraft:player} */
    String getType();

    /** @return whether the entity is a player */
    boolean isPlayer();

    /** @return the name of the world the entity is in */
    String getWorldName();

    double getX();

    double getY();

    double getZ();

    /**
     * The block at the entity's position moved by the offset; (0, -0.1, 0) is the block the
     * entity stands on, (0, 0, 0) the block its feet are inside, such as a stonecutter or a
     * slab.
     *
     * @return the block id, such as {@code minecraft:stonecutter}, or {@code null} if that
     *         block is not loaded
     */
    String getBlockAt(double dx, double dy, double dz);

    /**
     * Whether the entity, if it is a player, has the permission node. Non-players have none.
     *
     * @param permission the node
     * @return the platform's answer, as for a command
     */
    boolean hasPermission(String permission);
}
