package net.streamline.api.interfaces.audiences.permissions;

public interface IPermissionHolder {
    boolean hasPermission(String permission);

    void addPermission(String permission);

    void removePermission(String permission);
}
