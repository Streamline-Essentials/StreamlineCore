package net.streamline.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import singularity.client.ClientInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Client calls whose shape differs between Minecraft versions; this is Minecraft 1.8.9's.
 */
public final class ClientCompat {

    private static final ClientInfo INFO = new LegacyClientInfo();
    private static final List<Runnable> LATER = new ArrayList<Runnable>();

    private ClientCompat() {}

    /** @return what the client knows about itself, read live */
    public static ClientInfo clientInfo() {
        return INFO;
    }

    /** @return the open screen, or {@code null} in game */
    public static Object currentScreen() {
        return Minecraft.getMinecraft().currentScreen;
    }

    /** @param screen a {@code GuiScreen}, or {@code null} to close the open one */
    public static void setScreen(Object screen) {
        Minecraft.getMinecraft().displayGuiScreen((GuiScreen) screen);
    }

    /**
     * Runs {@code task} on the client thread at the end of the next client tick, even when
     * called from the client thread. 1.8.9's {@code addScheduledTask} runs a task at once when
     * already on that thread, which would open a screen while the chat screen that ran the
     * command is still closing itself.
     */
    public static void later(Runnable task) {
        synchronized (LATER) {
            LATER.add(task);
        }
    }

    /** Runs what {@link #later} queued; the client tick handler calls it. */
    public static void runQueued() {
        List<Runnable> tasks;
        synchronized (LATER) {
            if (LATER.isEmpty()) return;
            tasks = new ArrayList<Runnable>(LATER);
            LATER.clear();
        }
        for (Runnable task : tasks) task.run();
    }

    /** @return whether the left mouse button is held now */
    public static boolean leftMouseDown() {
        return Mouse.isButtonDown(0);
    }

    /** @return whether the player hid the GUI (F1) */
    public static boolean hudHidden() {
        return Minecraft.getMinecraft().gameSettings.hideGUI;
    }
}
