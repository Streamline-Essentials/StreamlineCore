package singularity.interfaces.audiences.permissions;

/**
 * Represents an entity that holds and can be queried for permission nodes.
 *
 * <p>Implementations are responsible for tracking which permission strings have been
 * granted to this holder and for checking whether a given permission is present.</p>
 */
public interface IPermissionHolder {

    /**
     * Returns whether this holder has been granted the specified permission node.
     *
     * @param permission the permission node to test, e.g. {@code "streamline.admin"}
     * @return {@code true} if the permission is granted; {@code false} otherwise
     */
    boolean hasPermission(String permission);

    /**
     * Grants the specified permission node to this holder.
     *
     * @param permission the permission node to add
     */
    void addPermission(String permission);

    /**
     * Revokes the specified permission node from this holder.
     *
     * @param permission the permission node to remove
     */
    void removePermission(String permission);
}
