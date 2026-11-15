package net.streamline.platform.handlers.gui;

import host.plas.bou.scheduling.TaskManager;
import host.plas.bou.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import singularity.data.players.CosmicPlayer;
import singularity.gui.GuiView;
import singularity.gui.IGuiHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders {@link singularity.gui.CosmicGui}s as BukkitOfUtils {@link CosmicScreen}s. All
 * inventory work runs on the player's own thread through BukkitOfUtils' scheduler, which covers
 * Folia.
 *
 * <p>Each player's current screen is tracked here. A screen reports its close only while it is
 * still the current one, so a screen replaced by another — including a reopen to change the
 * title or size — never reports a close for the GUI that replaced it.</p>
 */
public class SpigotGuiHandler implements IGuiHandler {
    private final Map<UUID, CosmicScreen> current = new ConcurrentHashMap<>();
    private volatile boolean canRetitle = true;

    @Override
    public boolean open(CosmicPlayer viewer, GuiView view) {
        Player player = player(viewer);
        if (player == null) return false;

        TaskManager.schedule(player, () -> display(player, view));
        return true;
    }

    @Override
    public boolean update(CosmicPlayer viewer, GuiView view) {
        return open(viewer, view);
    }

    @Override
    public void close(CosmicPlayer viewer) {
        Player player = player(viewer);
        if (player == null) return;

        TaskManager.schedule(player, () -> {
            CosmicScreen screen = current.remove(player.getUniqueId());
            if (screen != null && isShowing(player, screen)) player.closeInventory();
        });
    }

    /** Drops {@code screen} if it is its player's current screen, returning whether it was. */
    boolean release(CosmicScreen screen) {
        return current.remove(screen.getPlayer().getUniqueId(), screen);
    }

    /**
     * Shows {@code view} in the player's open Streamline screen when it has the same number of
     * rows, swapping the items (and the title, where the server can) in place, so going from one
     * GUI to another neither closes the window nor moves the cursor. Otherwise opens a new screen.
     */
    private void display(Player player, GuiView view) {
        CosmicScreen screen = current.get(player.getUniqueId());
        if (screen == null || ! isShowing(player, screen) || ! screen.getView().isLayoutCompatible(view)) {
            show(player, view);
            return;
        }

        boolean retitle = ! screen.getView().getTitle().equals(view.getTitle());
        if (retitle && ! canRetitle) {
            show(player, view);
            return;
        }

        screen.apply(view);
        if (retitle) {
            try {
                player.getOpenInventory().setTitle(ColorUtils.colorizeHard(view.getTitle()));
            } catch (NoSuchMethodError | UnsupportedOperationException | IllegalArgumentException e) {
                // InventoryView#setTitle arrived in 1.19.4; older servers reopen to change titles.
                canRetitle = false;
                show(player, view);
            }
        }
    }

    private void show(Player player, GuiView view) {
        if (! player.isOnline()) return;

        CosmicScreen screen = new CosmicScreen(player, view, this);
        current.put(player.getUniqueId(), screen);
        screen.open();
    }

    private static boolean isShowing(Player player, CosmicScreen screen) {
        return player.getOpenInventory().getTopInventory().equals(screen.getInventory());
    }

    private static Player player(CosmicPlayer viewer) {
        if (viewer == null) return null;
        try {
            return Bukkit.getPlayer(UUID.fromString(viewer.getUuid()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
