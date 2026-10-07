package singularity.gui;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.gui.transport.GuiMessages;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import singularity.utils.profiles.CosmicProfile;
import singularity.utils.profiles.PlayerLookup;

/**
 * Opens {@link CosmicGui}s and routes what players do in them back to the GUI's handlers.
 *
 * <p>On a backend with an {@link IGuiHandler}, GUIs are rendered locally. On a proxy, the GUI's
 * {@linkplain CosmicGui#snapshot() view} is sent to the player's backend, which renders it and
 * reports clicks and closes back; the handlers stay on the proxy and run there.</p>
 *
 * <p>Two kinds of state are kept per viewer:</p>
 * <ul>
 *     <li>a <em>session</em>: the GUI this server built and has open for the viewer, rendered
 *     here or remotely;</li>
 *     <li>a <em>hosted view</em>: on a backend, the id of a GUI the proxy built that this server
 *     is rendering, whose clicks are forwarded to the proxy.</li>
 * </ul>
 * <p>A viewer has at most one of the two. Clicks and closes carry the id of the GUI they happened
 * in, and are ignored unless it is the one the viewer currently has, so a late event from a
 * replaced screen never reaches the GUI that replaced it.</p>
 */
public final class GuiManager {
    private GuiManager() {
    }

    /** The local renderer; {@code null} on proxies. */
    @Getter @Setter
    private static IGuiHandler handler;

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static final Map<String, String> hostedViews = new ConcurrentHashMap<>();

    @Getter
    private static final class Session {
        private final CosmicGui gui;
        /** The backend rendering this session, or {@code null} when rendered locally. */
        private final String remoteServer;

        private Session(CosmicGui gui, String remoteServer) {
            this.gui = gui;
            this.remoteServer = remoteServer;
        }

        private boolean isRemote() {
            return remoteServer != null;
        }
    }

    /** Whether this server can show GUIs, locally or by handing them to a backend. */
    public static boolean isAvailable() {
        return handler != null || Singularity.isProxy();
    }

    /**
     * Lays {@code gui} out for {@code viewer} and opens it, replacing whatever GUI they had open.
     *
     * @return whether the GUI was handed to a renderer
     */
    public static boolean open(CosmicPlayer viewer, CosmicGui gui) {
        if (viewer == null || gui == null || viewer.isConsole()) return false;
        String uuid = viewer.getUuid();

        GuiView view = drawView(viewer, gui);
        if (view == null) return false;

        if (handler != null) {
            String hosted = hostedViews.remove(uuid);
            if (hosted != null) GuiMessages.sendClosed(viewer, hosted);

            sessions.put(uuid, new Session(gui, null));
            return handler.open(viewer, view);
        }

        if (Singularity.isProxy()) {
            String server = viewer.getServerName();
            sessions.put(uuid, new Session(gui, server == null ? "" : server));
            if (GuiMessages.sendOpen(viewer, view, false)) return true;
            sessions.remove(uuid);
            return false;
        }

        MessageUtils.logWarning("Cannot open GUI '" + gui.getTitle() + "' for " + viewer.getCurrentName()
                + ": this platform has no GUI renderer.");
        return false;
    }

    /** Re-lays {@code gui} out and resends it to everyone viewing it. */
    public static void redraw(CosmicGui gui) {
        if (gui == null) return;

        for (Map.Entry<String, Session> entry : sessions.entrySet()) {
            Session session = entry.getValue();
            if (session.getGui() != gui) continue;

            CosmicPlayer viewer = UserUtils.getOrCreatePlayer(entry.getKey()).orElse(null);
            if (viewer == null || ! viewer.isOnline()) {
                sessions.remove(entry.getKey(), session);
                continue;
            }

            if (session.isRemote() && ! Objects.equals(session.getRemoteServer(), viewer.getServerName())) {
                // The viewer changed servers; the backend that rendered this GUI is gone.
                sessions.remove(entry.getKey(), session);
                continue;
            }

            GuiView view = drawView(viewer, gui);
            if (view == null) continue;

            if (session.isRemote()) GuiMessages.sendOpen(viewer, view, true);
            else if (handler != null) handler.update(viewer, view);
        }
    }

    /** Closes whatever GUI {@code viewer} has open, without running its close handler. */
    public static void close(CosmicPlayer viewer) {
        if (viewer == null) return;
        String uuid = viewer.getUuid();

        Session session = sessions.remove(uuid);
        hostedViews.remove(uuid);

        if (session != null && session.isRemote()) {
            GuiMessages.sendClose(viewer);
        } else if (handler != null) {
            handler.close(viewer);
        }
    }

    /** The GUI this server built that {@code viewer} has open. */
    public static Optional<CosmicGui> getOpenGui(String viewerUuid) {
        Session session = viewerUuid == null ? null : sessions.get(viewerUuid);
        return session == null ? Optional.empty() : Optional.of(session.getGui());
    }

    public static List<CosmicPlayer> getViewers(CosmicGui gui) {
        List<CosmicPlayer> viewers = new ArrayList<>();
        sessions.forEach((uuid, session) -> {
            if (session.getGui() == gui) UserUtils.getOrCreatePlayer(uuid).ifPresent(viewers::add);
        });
        return viewers;
    }

    /**
     * Called by renderers when a viewer clicks a slot. Forwards the click to the proxy for a
     * hosted view, or runs the clicked icon's handler for a GUI built here.
     */
    public static void handleClick(String viewerUuid, String guiId, int slot, GuiClickType type) {
        if (viewerUuid == null || guiId == null) return;

        String hosted = hostedViews.get(viewerUuid);
        if (guiId.equals(hosted)) {
            CosmicPlayer viewer = UserUtils.getOrCreatePlayer(viewerUuid).orElse(null);
            if (viewer != null) GuiMessages.sendClick(viewer, guiId, slot, type);
            return;
        }

        Session session = sessions.get(viewerUuid);
        if (session == null || ! session.getGui().getId().equals(guiId)) return;

        GuiIcon icon = session.getGui().getIcon(slot).orElse(null);
        if (icon == null || ! icon.isClickable()) return;

        CosmicPlayer viewer = UserUtils.getOrCreatePlayer(viewerUuid).orElse(null);
        if (viewer == null) return;

        try {
            icon.getOnClick().accept(new GuiClick(viewer, session.getGui(), slot, type));
        } catch (Throwable t) {
            MessageUtils.logWarning("A click handler in GUI '" + session.getGui().getTitle() + "' failed", t);
        }
    }

    /**
     * Called by renderers when a viewer's screen showing {@code guiId} closes, whether they closed
     * it or a plugin did.
     */
    public static void handleClosed(String viewerUuid, String guiId) {
        if (viewerUuid == null || guiId == null) return;

        String hosted = hostedViews.get(viewerUuid);
        if (guiId.equals(hosted)) {
            hostedViews.remove(viewerUuid, hosted);
            CosmicPlayer viewer = UserUtils.getOrCreatePlayer(viewerUuid).orElse(null);
            if (viewer != null) GuiMessages.sendClosed(viewer, guiId);
            return;
        }

        Session session = sessions.get(viewerUuid);
        if (session == null || ! session.getGui().getId().equals(guiId)) return;
        if (! sessions.remove(viewerUuid, session)) return;

        if (session.getGui().getOnClose() == null) return;
        CosmicPlayer viewer = UserUtils.getOrCreatePlayer(viewerUuid).orElse(null);
        if (viewer == null) return;
        try {
            session.getGui().getOnClose().accept(viewer);
        } catch (Throwable t) {
            MessageUtils.logWarning("The close handler of GUI '" + session.getGui().getTitle() + "' failed", t);
        }
    }

    /**
     * Renders a view the proxy sent. The proxy's GUI replaces any GUI this server had open for the
     * viewer.
     */
    public static void hostRemote(CosmicPlayer viewer, GuiView view, boolean update) {
        if (viewer == null || view == null) return;
        if (handler == null) {
            MessageUtils.logWarning("The proxy sent a GUI for " + viewer.getCurrentName()
                    + ", but this server has no GUI renderer.");
            return;
        }

        String uuid = viewer.getUuid();
        sessions.remove(uuid);

        // An update for a view the player already closed must not reopen it.
        if (update && ! view.getId().equals(hostedViews.get(uuid))) return;

        hostedViews.put(uuid, view.getId());
        if (update) handler.update(viewer, view);
        else handler.open(viewer, view);
    }

    /** Closes a view the proxy sent, at the proxy's request. */
    public static void closeRemote(CosmicPlayer viewer) {
        if (viewer == null) return;
        if (hostedViews.remove(viewer.getUuid()) != null && handler != null) handler.close(viewer);
    }

    /** Drops all state for a viewer who left. */
    public static void forget(String viewerUuid) {
        if (viewerUuid == null) return;
        sessions.remove(viewerUuid);
        hostedViews.remove(viewerUuid);
    }

    private static GuiView drawView(CosmicPlayer viewer, CosmicGui gui) {
        try {
            gui.draw(viewer);
            GuiView view = gui.snapshot();
            applySkins(gui, view);
            return view;
        } catch (Throwable t) {
            MessageUtils.logWarning("Failed to draw GUI '" + gui.getTitle() + "' for " + viewer.getCurrentName(), t);
            return null;
        }
    }

    /**
     * Gives every player head in {@code view} the skin {@link PlayerLookup} has cached for its
     * owner, so renderers never ask a session server for it (Mojang's knows no Bedrock player
     * and rate-limits busy servers). Heads whose owner is not cached yet show the default skin;
     * once their lookups finish with a skin, the GUI is redrawn for whoever still has it open.
     */
    private static void applySkins(CosmicGui gui, GuiView view) {
        List<CompletableFuture<Optional<CosmicProfile>>> pending = new ArrayList<>();
        for (Map.Entry<Integer, CosmicItem> entry : new ArrayList<>(view.getItems().entrySet())) {
            int slot = entry.getKey();
            CosmicItem item = entry.getValue();
            if (item == null || item.getSkullOwner() == null || item.getSkinTexture() != null) continue;

            CompletableFuture<Optional<CosmicProfile>> profile = profileOf(item.getSkullOwner());
            if (profile.isDone()) {
                profile.getNow(Optional.empty()).filter(CosmicProfile::hasTextures)
                        .ifPresent(found -> view.getItems().put(slot, item.copy().setSkinTexture(found.getTexturesValue())));
            } else {
                pending.add(profile);
            }
        }
        if (pending.isEmpty()) return;

        CompletableFuture.allOf(pending.toArray(new CompletableFuture[0])).thenRun(() -> {
            boolean found = false;
            for (CompletableFuture<Optional<CosmicProfile>> profile : pending) {
                if (profile.getNow(Optional.empty()).filter(CosmicProfile::hasTextures).isPresent()) found = true;
            }
            if (found && ! getViewers(gui).isEmpty()) redraw(gui);
        });
    }

    /** The profile of a head's owner, a UUID or a name. Offline-mode UUIDs are looked up through their loaded name. */
    private static CompletableFuture<Optional<CosmicProfile>> profileOf(String owner) {
        UUID uuid;
        try {
            uuid = UUID.fromString(owner);
        } catch (IllegalArgumentException notUuid) {
            return PlayerLookup.lookupByName(owner);
        }
        String name = UserUtils.getLoadedSenders().containsKey(owner) ? UserUtils.getLoadedSenders().get(owner).getCurrentName() : null;
        return PlayerLookup.lookup(uuid, name);
    }
}
