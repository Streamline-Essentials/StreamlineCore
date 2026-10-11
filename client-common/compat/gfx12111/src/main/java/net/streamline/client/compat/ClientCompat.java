package net.streamline.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;
import net.streamline.client.LiveClientInfo;
import net.streamline.client.ModernKeys;
import org.lwjgl.glfw.GLFW;
import singularity.client.ClientInfo;
import singularity.client.input.ClientInput;

/**
 * Client calls whose shape differs between Minecraft versions; this is Minecraft 1.21.11's.
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
        return minecraft().screen;
    }

    /** @param screen a {@code Screen}, or {@code null} to close the open one */
    public static void setScreen(Object screen) {
        minecraft().setScreen((Screen) screen);
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
     * @return whether the left mouse button is held now, read from GLFW: {@code MouseHandler}
     * tracks its buttons only while no screen is open
     */
    public static boolean leftMouseDown() {
        long window = minecraft().getWindow().handle();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    /** @return whether the left Shift key is held now, read from GLFW */
    public static boolean leftShiftDown() {
        long window = minecraft().getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
    }

    /** @return whether the key with this GLFW key code is held now */
    public static boolean keyCodeDown(int code) {
        long window = minecraft().getWindow().handle();
        return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
    }

    /** @return whether the keyboard key with this name ({@code key.keyboard.g}) is held now */
    public static boolean keyDown(String name) {
        return ModernKeys.down(name);
    }

    /** @return the key's name as the Controls screen shows it, or {@code null} if it is unknown */
    public static String keyDisplayName(String name) {
        return ModernKeys.displayName(name);
    }

    /** Presses or releases a game key for client modules; see {@link ModernKeys#hold}. */
    public static void holdGameKey(ClientInput.GameKey key, boolean held) {
        ModernKeys.hold(key, held);
    }
    /** @return whether the player hid the GUI (F1) */
    public static boolean hudHidden() {
        return minecraft().options.hideGui;
    }

    /** @return the world's time of day in ticks, counting up across days */
    public static long dayTime(Level level) {
        return level.getDayTime();
    }

    /** @return the dimension id, such as {@code minecraft:overworld} */
    public static String dimensionId(Level level) {
        return level.dimension().identifier().toString();
    }
}
