package net.streamline.api.permissions;

import net.streamline.base.Streamline;
import net.streamline.api.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

/**
 * Base Permissible for use in any Permissible object via proxy or extension
 */
public class PermissibleBase implements Permissible {
    private final ServerOperator opable;
    private final Permissible parent;
    private final List<PermissionAttachment> attachments = new LinkedList<PermissionAttachment>();
    private final Map<String, PermissionAttachmentInfo> permissions = new HashMap<String, PermissionAttachmentInfo>();

    public PermissibleBase(@Nullable ServerOperator opable) {
        this.opable = opable;
        this.parent = (opable instanceof Permissible) ? (Permissible) opable : this;

        recalculatePermissions();
    }

    @Override
    public boolean isOp() {
        if (opable == null) {
            return false;
        } else {
            return opable.isOp();
        }
    }

    @Override
    public void setOp(boolean value) {
        if (opable == null) {
            throw new UnsupportedOperationException("Cannot change op value as no ServerOperator is set");
        } else {
            opable.setOp(value);
        }
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }

        return permissions.containsKey(name.toLowerCase(java.util.Locale.ENGLISH));
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        if (perm == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }

        return isPermissionSet(perm.getName());
    }

    @Override
    public boolean hasPermission(@NotNull String inName) {
        if (inName == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }

        String name = inName.toLowerCase(java.util.Locale.ENGLISH);

        if (isPermissionSet(name)) {
            return permissions.get(name).getValue();
        } else {
            Permission perm = Streamline.getInstance().getModuleManager().getPermission(name);

            if (perm != null) {
                return perm.getDefault().getValue(isOp());
            } else {
                return Permission.DEFAULT_PERMISSION.getValue(isOp());
            }
        }
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        if (perm == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }

        String name = perm.getName().toLowerCase(java.util.Locale.ENGLISH);

        if (isPermissionSet(name)) {
            return permissions.get(name).getValue();
        }
        return perm.getDefault().getValue(isOp());
    }

    @Override
    @NotNull
    public PermissionAttachment addAttachment(@NotNull Module module, @NotNull String name, boolean value) {
        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        } else if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        } else if (! module.isEnabled()) {
            throw new IllegalArgumentException("Module " + module.getDescription().getFullName() + " is disabled");
        }

        PermissionAttachment result = addAttachment(module);
        result.setPermission(name, value);

        recalculatePermissions();

        return result;
    }

    @Override
    @NotNull
    public PermissionAttachment addAttachment(@NotNull Module module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        } else if (!module.isEnabled()) {
            throw new IllegalArgumentException("Module " + module.getDescription().getFullName() + " is disabled");
        }

        PermissionAttachment result = new PermissionAttachment(module, parent);

        attachments.add(result);
        recalculatePermissions();

        return result;
    }

    @Override
    public void removeAttachment(@NotNull PermissionAttachment attachment) {
        if (attachment == null) {
            throw new IllegalArgumentException("Attachment cannot be null");
        }

        if (attachments.remove(attachment)) {
            PermissionRemovedExecutor ex = attachment.getRemovalCallback();

            if (ex != null) {
                ex.attachmentRemoved(attachment);
            }

            recalculatePermissions();
        } else {
            throw new IllegalArgumentException("Given attachment is not part of Permissible object " + parent);
        }
    }

    @Override
    public void recalculatePermissions() {
        clearPermissions();
        Set<Permission> defaults = Streamline.getInstance().getModuleManager().getDefaultPermissions(isOp());
        Streamline.getInstance().getModuleManager().subscribeToDefaultPerms(isOp(), parent);

        for (Permission perm : defaults) {
            String name = perm.getName().toLowerCase(java.util.Locale.ENGLISH);
            permissions.put(name, new PermissionAttachmentInfo(parent, name, null, true));
            Streamline.getInstance().getModuleManager().subscribeToPermission(name, parent);
            calculateChildPermissions(perm.getChildren(), false, null);
        }

        for (PermissionAttachment attachment : attachments) {
            calculateChildPermissions(attachment.getPermissions(), false, attachment);
        }
    }

    public synchronized void clearPermissions() {
        Set<String> perms = permissions.keySet();

        for (String name : perms) {
            Streamline.getInstance().getModuleManager().unsubscribeFromPermission(name, parent);
        }

        Streamline.getInstance().getModuleManager().unsubscribeFromDefaultPerms(false, parent);
        Streamline.getInstance().getModuleManager().unsubscribeFromDefaultPerms(true, parent);

        permissions.clear();
    }

    private void calculateChildPermissions(@NotNull Map<String, Boolean> children, boolean invert, @Nullable PermissionAttachment attachment) {
        for (Map.Entry<String, Boolean> entry : children.entrySet()) {
            String name = entry.getKey();

            Permission perm = Streamline.getInstance().getModuleManager().getPermission(name);
            boolean value = entry.getValue() ^ invert;
            String lname = name.toLowerCase(java.util.Locale.ENGLISH);

            permissions.put(lname, new PermissionAttachmentInfo(parent, lname, attachment, value));
            Streamline.getInstance().getModuleManager().subscribeToPermission(name, parent);

            if (perm != null) {
                calculateChildPermissions(perm.getChildren(), !value, attachment);
            }
        }
    }

    @Override
    @Nullable
    public PermissionAttachment addAttachment(@NotNull Module module, @NotNull String name, boolean value, int ticks) {
        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        } else if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        } else if (!module.isEnabled()) {
            throw new IllegalArgumentException("Module " + module.getDescription().getFullName() + " is disabled");
        }

        PermissionAttachment result = addAttachment(module, ticks);

        if (result != null) {
            result.setPermission(name, value);
        }

        return result;
    }

    @Override
    @Nullable
    public PermissionAttachment addAttachment(@NotNull Module module, int ticks) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        } else if (!module.isEnabled()) {
            throw new IllegalArgumentException("Module " + module.getDescription().getFullName() + " is disabled");
        }

        PermissionAttachment result = addAttachment(module);

        if (Streamline.getInstance().getScheduler().scheduleSyncDelayedTask(module, new RemoveAttachmentRunnable(result), ticks) == -1) {
            Streamline.getInstance().getLogger().log(Level.WARNING, "Could not add PermissionAttachment to " + parent + " for module " + module.getDescription().getFullName() + ": Scheduler returned -1");
            result.remove();
            return null;
        } else {
            return result;
        }
    }

    @Override
    @NotNull
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return new HashSet<PermissionAttachmentInfo>(permissions.values());
    }

    private static class RemoveAttachmentRunnable implements Runnable {
        private final PermissionAttachment attachment;

        public RemoveAttachmentRunnable(@NotNull PermissionAttachment attachment) {
            this.attachment = attachment;
        }

        @Override
        public void run() {
            attachment.remove();
        }
    }
}
