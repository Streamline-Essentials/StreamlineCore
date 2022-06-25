package net.streamline.api.entities;

import net.md_5.bungee.api.chat.BaseComponent;
import net.streamline.api.BasePlugin;
import net.streamline.api.command.IConsoleCommandSender;
import net.streamline.api.modules.Module;
import net.streamline.api.permissions.Permission;
import net.streamline.api.permissions.PermissionAttachment;
import net.streamline.api.permissions.PermissionAttachmentInfo;
import net.streamline.base.Streamline;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ConsoleCommandSender implements IConsoleCommandSender {
    public boolean op = true;

    @Override
    public String getName() {
        return Streamline.getInstance().getProxy().getConsole().getName();
    }

    @Override
    public @NotNull String getUUID() {
        return null;
    }

    @Override
    public void sendMessage(String message) {
        Streamline.getInstance().getProxy().getConsole().sendMessage(message);
    }

    @Override
    public void sendMessage(@NotNull String... messages) {
        Streamline.getInstance().getProxy().getConsole().sendMessages(messages);
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String message) {
        Streamline.getInstance().getProxy().getConsole().sendMessage(message);
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String... messages) {
        Streamline.getInstance().getProxy().getConsole().sendMessages(messages);
    }

    @Override
    public @NotNull BasePlugin getBase() {
        return Streamline.getInstance();
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        return Streamline.getInstance().getProxy().getConsole().hasPermission(name);
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        return Streamline.getInstance().getProxy().getConsole().hasPermission(perm.getName());
    }

    @Override
    public boolean hasPermission(String permission) {
        return Streamline.getInstance().getProxy().getConsole().hasPermission(permission);
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        return Streamline.getInstance().getProxy().getConsole().hasPermission(perm.getName());
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Module module, @NotNull String name, boolean value) {
        return null;
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Module module) {
        return null;
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Module module, @NotNull String name, boolean value, int ticks) {
        return null;
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Module module, int ticks) {
        return null;
    }

    @Override
    public void removeAttachment(@NotNull PermissionAttachment attachment) {

    }

    @Override
    public void recalculatePermissions() {
        Streamline.getInstance().getProxy().getConsole().getPermissions();
    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return new HashSet<>();
    }

    @Override
    public boolean isOp() {
        return this.op;
    }

    @Override
    public void setOp(boolean value) {
        this.op = value;
    }
}
