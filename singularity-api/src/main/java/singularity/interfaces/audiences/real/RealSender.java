package singularity.interfaces.audiences.real;

import lombok.Getter;
import lombok.Setter;
import singularity.interfaces.audiences.getters.SenderGetter;
import singularity.interfaces.audiences.messaging.ICommandable;
import singularity.interfaces.audiences.messaging.IConsolable;
import singularity.interfaces.audiences.messaging.IMessagable;
import singularity.interfaces.audiences.permissions.IPermissionHolder;

/**
 * Abstract base representation of a real command-sender on any supported platform.
 *
 * <p>{@code RealSender} wraps a platform-specific sender object of type {@code C} behind a
 * {@link SenderGetter} supplier, providing a unified surface for messaging, command execution,
 * console output, and permission checks across Velocity, BungeeCord, and Spigot.</p>
 *
 * @param <C> the platform-native command-sender type (e.g. {@code CommandSource} on Velocity)
 */
@Getter @Setter
public abstract class RealSender<C> implements IMessagable, ICommandable, IConsolable, IPermissionHolder {

    /**
     * The supplier used to retrieve the underlying platform-native sender object on demand.
     */
    private final SenderGetter<C> senderGetter;

    /**
     * Constructs a {@code RealSender} backed by the given sender supplier.
     *
     * @param senderGetter a non-null supplier that returns the platform-native sender object
     */
    public RealSender(SenderGetter<C> senderGetter) {
        this.senderGetter = senderGetter;
    }

    /**
     * Returns the underlying platform-native sender object by invoking the {@link SenderGetter}.
     *
     * <p>Named {@code getConsole} because in most proxy platforms the console and player
     * share the same {@code CommandSender} hierarchy; use {@link RealPlayer#getPlayer()} for
     * the player-specific cast.</p>
     *
     * @return the platform-native sender, never {@code null} as long as the getter is valid
     */
    public C getConsole() {
        return senderGetter.get();
    }
}
