package net.streamline.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;
import net.streamline.client.LiveClientInfo;
import singularity.client.ClientInfo;

import java.lang.reflect.Field;

/**
 * Client calls whose shape differs between Minecraft versions; this is Minecraft 26.x's.
 */
public final class ClientCompat {

    private static final ClientInfo INFO = new LiveClientInfo();

    private ClientCompat() {}

    /** @return what the client knows about itself, read live */
    public static ClientInfo clientInfo() {
        return INFO;
    }

    private static Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    /** @return the open screen, or {@code null} in game */
    public static Object currentScreen() {
        return minecraft().gui.screen();
    }

    /** @param screen a {@code Screen}, or {@code null} to close the open one */
    public static void setScreen(Object screen) {
        minecraft().gui.setScreen((Screen) screen);
    }

    /**
     * Runs {@code task} on the client thread at its next task run, even when called from the
     * client thread itself. {@code Minecraft.execute} would run it right away there;
     * {@code schedule} always queues.
     */
    public static void later(Runnable task) {
        minecraft().schedule(task);
    }

    /**
     * {@code MouseHandler.activeButton}: the button held down, kept up to date while a screen
     * is open, unlike {@code isLeftPressed()}. 26.2 reads the mouse through GLFW and 26.3 through
     * SDL, so this field is the one way to ask that works on both; {@code null} if it is gone.
     */
    private static final Field ACTIVE_BUTTON = activeButtonField();

    private static Field activeButtonField() {
        try {
            Field field = MouseHandler.class.getDeclaredField("activeButton");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /**
     * @return whether a mouse button is held now; {@code true} when that cannot be read, so
     * callers fall back to the release event
     */
    public static boolean leftMouseDown() {
        if (ACTIVE_BUTTON == null) return true;
        try {
            return ACTIVE_BUTTON.get(minecraft().mouseHandler) != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return true;
        }
    }

    /** @return whether the player hid the GUI (F1) */
    public static boolean hudHidden() {
        return minecraft().gui.hud.isHidden();
    }

    /** @return the world's time of day in ticks, counting up across days */
    public static long dayTime(Level level) {
        return level.getDefaultClockTime();
    }

    /** @return the dimension id, such as {@code minecraft:overworld} */
    public static String dimensionId(Level level) {
        return level.dimension().identifier().toString();
    }
}
