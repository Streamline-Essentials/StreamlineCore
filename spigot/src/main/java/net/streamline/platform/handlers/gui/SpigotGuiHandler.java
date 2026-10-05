package net.streamline.platform.handlers.gui;

import host.plas.bou.scheduling.TaskManager;
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

    @Override
    public boolean open(CosmicPlayer viewer, GuiView view) {
        Player player = player(viewer);
        if (player == null) return false;

        TaskManager.schedule(player, () -> show(player, view));
        return true;
    }

    @Override
    public boolean update(CosmicPlayer viewer, GuiView view) {
        Player player = player(viewer);
        if (player == null) return false;

        TaskManager.schedule(player, () -> {
            CosmicScreen screen = current.get(player.getUniqueId());
            if (screen != null && isShowing(player, screen)
                    && screen.getView().getId().equals(view.getId())
                    && screen.getView().isLayoutCompatible(view)) {
                screen.apply(view);
            } else {
                show(player, view);
            }
        });
        return true;
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
