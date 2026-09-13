package singularity.data.players.location;

import lombok.Getter;

/**
 * Represents the angular rotation of a player or entity, comprising a yaw (horizontal)
 * and a pitch (vertical) angle expressed in degrees.
 *
 * <p>All mutating methods return {@code this} to support fluent method chaining.</p>
 */
@Getter
public class PlayerRotation {

    /**
     * Identifies which axis of rotation is being addressed.
     */
    public enum RotationType {
        /** Horizontal rotation around the vertical axis. */
        YAW,
        /** Vertical rotation around the horizontal axis. */
        PITCH,
        ;
    }

    /**
     * The horizontal rotation angle in degrees.
     * Managed by Lombok's {@code @Getter}; use {@link #setYaw(float)} to mutate.
     */
    private float yaw;

    /**
     * The vertical rotation angle in degrees.
     * Managed by Lombok's {@code @Getter}; use {@link #setPitch(float)} to mutate.
     */
    private float pitch;

    /**
     * Constructs a {@code PlayerRotation} with explicit yaw and pitch values.
     *
     * @param yaw   the horizontal rotation angle in degrees
     * @param pitch the vertical rotation angle in degrees
     */
    public PlayerRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * Constructs a default {@code PlayerRotation} with yaw and pitch both set to {@code 0}.
     */
    public PlayerRotation() {
        this(0, 0);
    }

    /**
     * Increases the yaw angle by the given amount and returns {@code this} for chaining.
     *
     * @param yaw the amount to add to the current yaw
     * @return this rotation instance
     */
    public PlayerRotation addYaw(float yaw) {
        this.yaw += yaw;

        return this;
    }

    /**
     * Increases the pitch angle by the given amount and returns {@code this} for chaining.
     *
     * @param pitch the amount to add to the current pitch
     * @return this rotation instance
     */
    public PlayerRotation addPitch(float pitch) {
        this.pitch += pitch;

        return this;
    }

    /**
     * Decreases the yaw angle by the given amount and returns {@code this} for chaining.
     *
     * @param yaw the amount to subtract from the current yaw
     * @return this rotation instance
     */
    public PlayerRotation removeYaw(float yaw) {
        this.yaw -= yaw;

        return this;
    }

    /**
     * Decreases the pitch angle by the given amount and returns {@code this} for chaining.
     *
     * @param pitch the amount to subtract from the current pitch
     * @return this rotation instance
     */
    public PlayerRotation removePitch(float pitch) {
        this.pitch -= pitch;

        return this;
    }

    /**
     * Sets the yaw angle to the given value and returns {@code this} for chaining.
     *
     * @param yaw the new yaw in degrees
     * @return this rotation instance
     */
    public PlayerRotation setYaw(float yaw) {
        this.yaw = yaw;

        return this;
    }

    /**
     * Sets the pitch angle to the given value and returns {@code this} for chaining.
     *
     * @param pitch the new pitch in degrees
     * @return this rotation instance
     */
    public PlayerRotation setPitch(float pitch) {
        this.pitch = pitch;

        return this;
    }

    /**
     * Returns the rotation value for the specified axis.
     *
     * @param type the axis whose value should be retrieved
     * @return the yaw if {@code type} is {@link RotationType#YAW}, otherwise the pitch
     */
    public float get(RotationType type) {
        return type == RotationType.YAW ? yaw : pitch;
    }

    /**
     * Sets the rotation value for the specified axis and returns {@code this} for chaining.
     *
     * @param type  the axis to modify
     * @param value the new angle value in degrees
     * @return this rotation instance
     */
    public PlayerRotation set(RotationType type, float value) {
        if (type == RotationType.YAW) {
            setYaw(value);
        } else {
            setPitch(value);
        }

        return this;
    }

    /**
     * Adds the given value to the rotation on the specified axis and returns {@code this} for chaining.
     *
     * @param type  the axis to modify
     * @param value the amount to add
     * @return this rotation instance
     */
    public PlayerRotation add(RotationType type, float value) {
        if (type == RotationType.YAW) {
            addYaw(value);
        } else {
            addPitch(value);
        }

        return this;
    }

    /**
     * Subtracts the given value from the rotation on the specified axis and returns {@code this} for chaining.
     *
     * @param type  the axis to modify
     * @param value the amount to subtract
     * @return this rotation instance
     */
    public PlayerRotation remove(RotationType type, float value) {
        if (type == RotationType.YAW) {
            removeYaw(value);
        } else {
            removePitch(value);
        }

        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the rotation as {@code "<yaw>;<pitch>"}.</p>
     */
    @Override
    public String toString() {
        return getYaw() + ";" + getPitch();
    }
}
