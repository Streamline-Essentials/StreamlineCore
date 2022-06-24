package net.streamline.api.entities;

import net.streamline.api.BasePlugin;
import net.streamline.api.command.CommandSender;
import net.streamline.api.modules.Module;
import net.streamline.api.permissions.Permission;
import net.streamline.api.permissions.PermissionAttachment;
import net.streamline.api.permissions.PermissionAttachmentInfo;
import net.streamline.api.savables.UserManager;
import net.streamline.utils.MessagingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;

public class Player implements IPlayer {
    private BasePlugin plugin;
    private String name;
    private String uuid;
    private String displayName;

    public Player(BasePlugin plugin, String name, String uuid) {
        this.plugin = plugin;
        this.name = name;
        this.uuid = uuid;
        this.displayName = UserManager.getDisplayName(this.name, this.name);
    }

    @Override
    public void sendMessage(@NotNull String message) {
        MessagingUtils.sendMessage(this.getUUID(), message);
    }

    @Override
    public void sendMessage(@NotNull String... messages) {
        MessagingUtils.sendMessage(this.getUUID(), MessagingUtils.normalize(messages));
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String message) {
        MessagingUtils.sendMessage(this.getUUID(), message);
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String... messages) {
        MessagingUtils.sendMessage(this.getUUID(), MessagingUtils.normalize(messages));
    }

    @Override
    public @NotNull BasePlugin getBase() {
        return this.plugin;
    }

    @Override
    public @NotNull String getName() {
        return this.name;
    }

    @Override
    public @NotNull String getUUID() {
        return this.uuid;
    }

    @Override
    public @NotNull String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(@Nullable String name) {
        this.displayName = name;
    }

    @Override
    public @NotNull String getPlayerListName() {
        return this.name;
    }

    @Override
    public void setPlayerListName(@Nullable String name) {

    }

    @Override
    public @Nullable String getPlayerListHeader() {
        return null;
    }

    @Override
    public @Nullable String getPlayerListFooter() {
        return null;
    }

    @Override
    public void setPlayerListHeader(@Nullable String header) {

    }

    @Override
    public void setPlayerListFooter(@Nullable String footer) {

    }

    @Override
    public void setPlayerListHeaderFooter(@Nullable String header, @Nullable String footer) {

    }

    @Override
    public @Nullable InetSocketAddress getAddress() {
        return null;
    }

    @Override
    public void sendRawMessage(@NotNull String message) {

    }

    @Override
    public void kickPlayer(@Nullable String message) {

    }

    @Override
    public void chat(@NotNull String msg) {

    }

    @Override
    public boolean performCommand(@NotNull String command) {
        return false;
    }

    @Override
    public void saveData() {

    }

    @Override
    public void loadData() {

    }

    @Override
    public void giveExp(int amount) {

    }

    @Override
    public void giveExpLevels(int amount) {

    }

    @Override
    public float getExp() {
        return 0;
    }

    @Override
    public void setExp(float exp) {

    }

    @Override
    public int getLevel() {
        return 0;
    }

    @Override
    public void setLevel(int level) {

    }

    @Override
    public int getTotalExperience() {
        return 0;
    }

    @Override
    public void setTotalExperience(int exp) {

    }

    @Override
    public void sendExperienceChange(float progress) {

    }

    @Override
    public void hidePlayer(@NotNull IPlayer IPlayer) {

    }

    @Override
    public void hidePlayer(@NotNull Module module, @NotNull IPlayer IPlayer) {

    }

    @Override
    public void showPlayer(@NotNull IPlayer IPlayer) {

    }

    @Override
    public void showPlayer(@NotNull Module module, @NotNull IPlayer IPlayer) {

    }

    @Override
    public boolean canSee(@NotNull IPlayer IPlayer) {
        return false;
    }

    @Override
    public void hideEntity(@NotNull Module module, @NotNull CommandSender entity) {

    }

    @Override
    public void showEntity(@NotNull Module module, @NotNull CommandSender entity) {

    }

    @Override
    public boolean canSee(@NotNull CommandSender entity) {
        return false;
    }

    @Override
    public void setTexturePack(@NotNull String url) {

    }

    @Override
    public void setResourcePack(@NotNull String url) {

    }

    @Override
    public void setResourcePack(@NotNull String url, @Nullable byte[] hash) {

    }

    @Override
    public void setResourcePack(@NotNull String url, @Nullable byte[] hash, @Nullable String prompt) {

    }

    @Override
    public void setResourcePack(@NotNull String url, @Nullable byte[] hash, boolean force) {

    }

    @Override
    public void setResourcePack(@NotNull String url, @Nullable byte[] hash, @Nullable String prompt, boolean force) {

    }

    @Override
    public void sendTitle(@Nullable String title, @Nullable String subtitle) {

    }

    @Override
    public void sendTitle(@Nullable String title, @Nullable String subtitle, int fadeIn, int stay, int fadeOut) {

    }

    @Override
    public void resetTitle() {

    }

    @Override
    public int getPing() {
        return 0;
    }

    @Override
    public @NotNull String getLocale() {
        return null;
    }

    @Override
    public void updateCommands() {

    }

    @Override
    public boolean isAllowingServerListings() {
        return false;
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        return false;
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        return false;
    }

    @Override
    public boolean hasPermission(@NotNull String name) {
        return false;
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        return false;
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

    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return null;
    }

    @Override
    public boolean isOp() {
        return false;
    }

    @Override
    public void setOp(boolean value) {

    }
}
