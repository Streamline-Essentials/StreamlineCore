package net.streamline.platform.handlers.gui;

import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.text.LegacyText;
import singularity.data.players.CosmicPlayer;
import singularity.gui.GuiView;
import singularity.gui.IGuiHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders {@link singularity.gui.CosmicGui}s as vanilla chest menus on Fabric, Forge and NeoForge.
 * All menu work runs on the server thread.
 *
 * <p>Each player's current menu is tracked here. A menu reports its close only while it is still
 * the current one, so a menu replaced by another — including a reopen to change the title or
 * size — never reports a close for the GUI that replaced it.</p>
 */
public class ModGuiHandler implements IGuiHandler {
    private final Map<String, CosmicMenu> current = new ConcurrentHashMap<>();

    @Override
    public boolean open(CosmicPlayer viewer, GuiView view) {
        if (viewer == null) return false;
        return runOnServer(() -> {
            ServerPlayer player = BasePlugin.getPlayer(viewer.getUuid());
            if (player != null) display(player, view);
        });
    }

    @Override
    public boolean update(CosmicPlayer viewer, GuiView view) {
        return open(viewer, view);
    }

    @Override
    public void close(CosmicPlayer viewer) {
        if (viewer == null) return;
        runOnServer(() -> {
            ServerPlayer player = BasePlugin.getPlayer(viewer.getUuid());
            if (player == null) return;

            CosmicMenu menu = current.remove(player.getStringUUID());
            if (menu != null && player.containerMenu == menu) player.closeContainer();
        });
    }

    /** Drops {@code menu} if it is its player's current menu, returning whether it was. */
    boolean release(CosmicMenu menu) {
        return current.remove(menu.getViewer().getStringUUID(), menu);
    }

    /**
     * Shows {@code view} in the player's open Streamline menu when it has the same number of rows,
     * swapping the items in place, so going from one GUI to another neither closes the window nor
     * moves the cursor. Otherwise opens a new menu.
     */
    private void display(ServerPlayer player, GuiView view) {
        CosmicMenu menu = current.get(player.getStringUUID());
        if (menu != null && player.containerMenu == menu && menu.getView().isLayoutCompatible(view)) {
            boolean retitle = ! menu.getView().getTitle().equals(view.getTitle());
            menu.apply(view);
            if (retitle) {
                // Reopening the same container id changes the client's title without the close
                // packet openMenu sends first.
                player.connection.send(new ClientboundOpenScreenPacket(menu.containerId, menu.getType(),
                        LegacyText.parse(view.getTitle())));
                menu.sendAllDataToRemote();
            }
        } else {
            show(player, view);
        }
    }

    private void show(ServerPlayer player, GuiView view) {
        // openMenu closes the open menu first; with nothing current, that close is not reported.
        current.remove(player.getStringUUID());
        player.openMenu(new SimpleMenuProvider((containerId, inventory, p) -> {
            CosmicMenu menu = CosmicMenu.create(containerId, inventory, player, view, this);
            current.put(player.getStringUUID(), menu);
            return menu;
        }, LegacyText.parse(view.getTitle())));
    }

    private static boolean runOnServer(Runnable task) {
        MinecraftServer server = BasePlugin.getServer();
        if (server == null) return false;
        if (server.isSameThread()) task.run();
        else server.execute(task);
        return true;
    }
}
