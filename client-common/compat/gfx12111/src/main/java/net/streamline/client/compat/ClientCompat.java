package net.streamline.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.Level;

/**
 * Client calls whose shape differs between Minecraft versions; this is Minecraft 1.21.11's.
 */
public final class ClientCompat {

    private ClientCompat() {}

    private static Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    /** @return the open screen, or {@code null} in game */
    public static Screen currentScreen() {
        return minecraft().screen;
    }

    public static void setScreen(Screen screen) {
        minecraft().setScreen(screen);
    }

    /**
     * Runs {@code task} on the client thread at its next task run, even when called from the
     * client thread itself. {@code Minecraft.execute} would run it right away there;
     * {@code schedule} always queues.
     */
    public static void later(Runnable task) {
        minecraft().schedule(task);
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
