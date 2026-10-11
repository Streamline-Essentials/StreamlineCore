package net.streamline.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;
import net.streamline.client.LiveClientInfo;
import singularity.client.ClientInfo;

import java.lang.reflect.Method;

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
     * Asks the window system whether the left button is held, without going through
     * {@code MouseHandler}: its button state comes from the events it was given, which other
     * mods can take or reorder. 26.2 reads input through GLFW and 26.3 through SDL, and this
     * source set compiles against both, so each is looked up by name; {@code null} if neither is
     * there.
     */
    private static final MouseQuery MOUSE = mouseQuery();

    private interface MouseQuery {
        boolean leftDown() throws Throwable;
    }

    private static MouseQuery mouseQuery() {
        try {
            // SDL_GetMouseState(NULL, NULL) returns the held buttons; bit 0 is the left one.
            Method state = Class.forName("org.lwjgl.sdl.SDLMouse").getMethod("nSDL_GetMouseState", long.class, long.class);
            return () -> (((Integer) state.invoke(null, 0L, 0L)) & 1) != 0;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // Not SDL: 26.2.
        }
        try {
            // glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
            Method button = Class.forName("org.lwjgl.glfw.GLFW").getMethod("glfwGetMouseButton", long.class, int.class);
            return () -> ((Integer) button.invoke(null, minecraft().getWindow().handle(), 0)) == 1;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    /**
     * @return whether the left mouse button is held now; {@code true} when that cannot be read,
     * so callers fall back to the release event
     */
    public static boolean leftMouseDown() {
        if (MOUSE == null) return true;
        try {
            return MOUSE.leftDown();
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * {@code InputConstants.isKeyDown}: {@code (Window, int)} on 26.2, {@code (int)} on 26.3, so it
     * is looked up rather than linked; {@code null} if neither shape is there.
     */
    private static final Method IS_KEY_DOWN = isKeyDownMethod();

    private static Method isKeyDownMethod() {
        for (Class<?>[] shape : new Class<?>[][] {{int.class}, {Window.class, int.class}}) {
            try {
                return InputConstants.class.getMethod("isKeyDown", shape);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    /** @return whether the left Shift key is held now; either Shift when that cannot be asked */
    public static boolean leftShiftDown() {
        int key = InputConstants.KEY_LSHIFT;
        try {
            if (IS_KEY_DOWN != null) {
                Object down = IS_KEY_DOWN.getParameterCount() == 1
                        ? IS_KEY_DOWN.invoke(null, key)
                        : IS_KEY_DOWN.invoke(null, minecraft().getWindow(), key);
                return (Boolean) down;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return minecraft().hasShiftDown();
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
