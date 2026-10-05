package net.streamline.api.permissions;

import net.streamline.api.SLAPI;
import singularity.utils.MessageUtils;
import singularity.utils.UuidUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * Streamline's permission entry point. It holds the active {@link PermissionProvider} and
 * makes every permission plugin a soft dependency: LuckPerms is used when it is installed
 * and {@link NoPermissionProvider} otherwise.
 *
 * <p>This class never links against a permission plugin's API itself. LuckPerms is probed
 * by name first; if the class is absent that result is final, while a LuckPerms that is
 * present but not yet enabled (it starts with the server on mod loaders, possibly after
 * Streamline) is retried on access, at most once every {@value #RETRY_MILLIS} ms.
 *
 * <p>Methods taking a {@code String} UUID accept any format {@link UuidUtils#toUuid}
 * understands and treat an unparseable one as an unknown player.
 */
public final class Permissions {
    private static final String LUCKPERMS_PROBE = "net.luckperms.api.LuckPermsProvider";
    private static final long RETRY_MILLIS = 5000L;

    private static volatile PermissionProvider provider = NoPermissionProvider.INSTANCE;
    private static volatile boolean luckPermsAbsent;
    private static volatile long nextAttempt;

    private Permissions() {}

    /**
     * @return the active provider, trying to hook LuckPerms first when it is not hooked yet
     */
    public static PermissionProvider getProvider() {
        PermissionProvider current = provider;
        if (current == NoPermissionProvider.INSTANCE && ! luckPermsAbsent && System.currentTimeMillis() >= nextAttempt) {
            return hook();
        }
        return current;
    }

    /**
     * Hooks LuckPerms if it is installed and enabled. A no-op once hooked.
     *
     * @return the active provider afterwards
     */
    public static synchronized PermissionProvider hook() {
        if (provider != NoPermissionProvider.INSTANCE || luckPermsAbsent) return provider;
        nextAttempt = System.currentTimeMillis() + RETRY_MILLIS;

        try {
            Class.forName(LUCKPERMS_PROBE, false, Permissions.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            luckPermsAbsent = true;
            return provider;
        }

        try {
            LuckPermsPermissionProvider luckPerms = LuckPermsPermissionProvider.create();
            SLAPI.setLpOptional(Optional.of(luckPerms.getApi()));
            provider = luckPerms;
            MessageUtils.logInfo("Hooked into LuckPerms for permissions.");
        } catch (IllegalStateException e) {
            // LuckPerms is installed but not enabled yet; retried after RETRY_MILLIS.
        } catch (LinkageError | RuntimeException e) {
            MessageUtils.logWarning("Could not hook into LuckPerms; permissions fall back to the platform's own.", e);
            luckPermsAbsent = true;
        }
        return provider;
    }

    /**
     * Drops the active provider, e.g. on disable, so a later {@link #hook()} binds afresh.
     */
    public static synchronized void unhook() {
        provider = NoPermissionProvider.INSTANCE;
        luckPermsAbsent = false;
        nextAttempt = 0L;
        SLAPI.setLpOptional(Optional.empty());
    }

    /**
     * @return whether a permission plugin backs Streamline's permissions
     */
    public static boolean isHooked() {
        return getProvider() != NoPermissionProvider.INSTANCE;
    }

    /**
     * @return whether permission changes take effect
     */
    public static boolean canModify() {
        return getProvider().canModify();
    }

    /**
     * @see PermissionProvider#permissionValue(UUID, String)
     */
    public static Optional<Boolean> permissionValue(String uuid, String permission) {
        UUID id = toUuid(uuid);
        return id == null ? Optional.empty() : getProvider().permissionValue(id, permission);
    }

    /**
     * @return whether the provider explicitly grants the node; {@code false} when unset
     */
    public static boolean hasPermission(String uuid, String permission) {
        return permissionValue(uuid, permission).orElse(false);
    }

    /**
     * @see PermissionProvider#addPermission(UUID, String)
     */
    public static boolean addPermission(String uuid, String permission) {
        UUID id = toUuid(uuid);
        return id != null && getProvider().addPermission(id, permission);
    }

    /**
     * @see PermissionProvider#removePermission(UUID, String)
     */
    public static boolean removePermission(String uuid, String permission) {
        UUID id = toUuid(uuid);
        return id != null && getProvider().removePermission(id, permission);
    }

    /**
     * @see PermissionProvider#getPrimaryGroup(UUID)
     */
    public static Optional<String> getPrimaryGroup(String uuid) {
        UUID id = toUuid(uuid);
        return id == null ? Optional.empty() : getProvider().getPrimaryGroup(id);
    }

    /**
     * @see PermissionProvider#getHighestGroup(UUID)
     */
    public static Optional<String> getHighestGroup(String uuid) {
        UUID id = toUuid(uuid);
        return id == null ? Optional.empty() : getProvider().getHighestGroup(id);
    }

    /**
     * @see PermissionProvider#getMeta(UUID, String)
     */
    public static Optional<String> getMeta(String uuid, String key) {
        UUID id = toUuid(uuid);
        return id == null ? Optional.empty() : getProvider().getMeta(id, key);
    }

    /**
     * @see PermissionProvider#getPrefix(UUID)
     */
    public static ChatMeta getPrefix(String uuid) {
        UUID id = toUuid(uuid);
        return id == null ? ChatMeta.EMPTY : getProvider().getPrefix(id);
    }

    /**
     * @see PermissionProvider#getSuffix(UUID)
     */
    public static ChatMeta getSuffix(String uuid) {
        UUID id = toUuid(uuid);
        return id == null ? ChatMeta.EMPTY : getProvider().getSuffix(id);
    }

    private static UUID toUuid(String uuid) {
        if (uuid == null) return null;
        String normalized = UuidUtils.toUuid(uuid);
        if (normalized == null) return null;
        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
