package net.streamline.api.data.players.location;

import lombok.Getter;

@Getter
public class PlayerRotation {
    public enum RotationType {
        YAW,
        PITCH,
        ;
    }

    private float yaw, pitch;

    public PlayerRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public PlayerRotation() {
        this(0, 0);
    }

    public PlayerRotation addYaw(float yaw) {
        this.yaw += yaw;

        return this;
    }

    public PlayerRotation addPitch(float pitch) {
        this.pitch += pitch;

        return this;
    }

    public PlayerRotation removeYaw(float yaw) {
        this.yaw -= yaw;

        return this;
    }

    public PlayerRotation removePitch(float pitch) {
        this.pitch -= pitch;

        return this;
    }

    public PlayerRotation setYaw(float yaw) {
        this.yaw = yaw;

        return this;
    }

    public PlayerRotation setPitch(float pitch) {
        this.pitch = pitch;

        return this;
    }

    public float get(RotationType type) {
        return type == RotationType.YAW ? yaw : pitch;
    }

    public PlayerRotation set(RotationType type, float value) {
        if (type == RotationType.YAW) {
            setYaw(value);
        } else {
            setPitch(value);
        }

        return this;
    }

    public PlayerRotation add(RotationType type, float value) {
        if (type == RotationType.YAW) {
            addYaw(value);
        } else {
            addPitch(value);
        }

        return this;
    }

    public PlayerRotation remove(RotationType type, float value) {
        if (type == RotationType.YAW) {
            removeYaw(value);
        } else {
            removePitch(value);
        }

        return this;
    }
}
