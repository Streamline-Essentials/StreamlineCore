package singularity.data.players.permissions;

import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import org.jetbrains.annotations.NotNull;

/**
 * Manages the permission state for a {@link CosmicSender}, including an optional
 * bypass flag that grants all permission checks unconditionally.
 *
 * <p>Permission additions and removals are delegated to the platform-native sender
 * returned by {@link CosmicSender#asReal()}.</p>
 */
@Getter @Setter
public class SenderPermissions implements Comparable<SenderPermissions> {

    /**
     * The sender whose permissions are managed by this object.
     */
    private CosmicSender sender;

    /**
     * When {@code true}, all calls to {@link #hasPermission(String)} return {@code true}
     * regardless of the actual permission node.
     */
    private boolean bypassingPermissions;

    /**
     * Constructs a {@code SenderPermissions} for the given sender with the bypass flag
     * initially set to {@code false}.
     *
     * @param sender the sender to manage permissions for
     */
    public SenderPermissions(CosmicSender sender) {
        this.sender = sender;
        this.bypassingPermissions = false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the underlying sender's natural ordering.</p>
     *
     * @param o the other {@code SenderPermissions} to compare against
     * @return the result of comparing the two senders
     */
    @Override
    public int compareTo(@NotNull SenderPermissions o) {
        return sender.compareTo(o.getSender());
    }

    /**
     * Returns whether the sender holds the specified permission node.
     *
     * <p>If {@link #isBypassingPermissions()} is {@code true}, this method always
     * returns {@code true} without consulting the platform.</p>
     *
     * @param permission the permission node to check
     * @return {@code true} if the sender has the permission (or bypass is active)
     */
    public boolean hasPermission(String permission) {
        if (isBypassingPermissions()) return true;

        return sender.asReal().hasPermission(permission);
    }

    /**
     * Grants the specified permission node to the sender via the platform layer.
     *
     * @param permission the permission node to add
     */
    public void addPermission(String permission) {
        sender.asReal().addPermission(permission);
    }

    /**
     * Revokes the specified permission node from the sender via the platform layer.
     *
     * @param permission the permission node to remove
     */
    public void removePermission(String permission) {
        sender.asReal().removePermission(permission);
    }
}
