package singularity.client.hud;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Undo and redo for the HUD layout: placements, frames and groups.
 *
 * <p>Call {@link #record()} right before changing the layout; it keeps a {@link HudRegistry}
 * snapshot to go back to. A snapshot equal to the last one kept is not kept again, and undo and
 * redo skip snapshots equal to the layout as it is, so a recorded change that changed nothing
 * costs no extra step. Everything runs on the render thread.</p>
 */
public final class HudHistory {

    /** Steps kept in each direction. */
    private static final int LIMIT = 100;

    private static final Deque<HudRegistry.Snapshot> UNDO = new ArrayDeque<>();
    private static final Deque<HudRegistry.Snapshot> REDO = new ArrayDeque<>();

    private HudHistory() {}

    /** Keeps the layout as it is now as the next step to undo, and forgets what could be redone. */
    public static synchronized void record() {
        HudRegistry.Snapshot now = HudRegistry.snapshot();
        if (! now.equals(UNDO.peek())) {
            UNDO.push(now);
            while (UNDO.size() > LIMIT) UNDO.removeLast();
        }
        REDO.clear();
    }

    /** @return whether a change was undone */
    public static synchronized boolean undo() {
        return step(UNDO, REDO);
    }

    /** @return whether a change was redone */
    public static synchronized boolean redo() {
        return step(REDO, UNDO);
    }

    private static boolean step(Deque<HudRegistry.Snapshot> from, Deque<HudRegistry.Snapshot> to) {
        HudRegistry.Snapshot now = HudRegistry.snapshot();
        while (! from.isEmpty()) {
            HudRegistry.Snapshot target = from.pop();
            if (target.equals(now)) continue;
            to.push(now);
            while (to.size() > LIMIT) to.removeLast();
            HudRegistry.restore(target);
            return true;
        }
        return false;
    }

    public static synchronized boolean canUndo() {
        return ! UNDO.isEmpty();
    }

    public static synchronized boolean canRedo() {
        return ! REDO.isEmpty();
    }

    /** Forgets every step. */
    public static synchronized void clear() {
        UNDO.clear();
        REDO.clear();
    }
}
