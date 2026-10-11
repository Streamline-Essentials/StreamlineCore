package host.plas.hud;

import singularity.client.hud.TextHudElement;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The look every StreamlineHUD element shares, set from the module's settings page. Values
 * change on whichever thread saves a setting and are read on the render thread, so they are
 * volatile and every change is pushed straight onto the registered elements.
 */
public final class HudStyle {

    private static final List<TextHudElement> ELEMENTS = new CopyOnWriteArrayList<>();

    private static volatile int textColor = 0xFFFFFFFF;
    private static volatile int backgroundColor = 0x60000000;
    private static volatile boolean background = true;
    private static volatile boolean shadow = true;
    private static volatile int coordinateDecimals = 1;
    private static volatile boolean twelveHourClock = false;

    private HudStyle() {}

    public static void track(TextHudElement element) {
        ELEMENTS.add(element);
        apply(element);
    }

    public static void clear() {
        ELEMENTS.clear();
    }

    private static void apply(TextHudElement element) {
        element.setTextColor(textColor);
        element.setBackground(background ? backgroundColor : 0);
        element.setShadow(shadow);
    }

    private static void applyAll() {
        for (TextHudElement element : ELEMENTS) apply(element);
    }

    public static void setTextColor(int argb) {
        textColor = argb;
        applyAll();
    }

    public static void setBackgroundColor(int argb) {
        backgroundColor = argb;
        applyAll();
    }

    public static void setBackground(boolean shown) {
        background = shown;
        applyAll();
    }

    public static void setShadow(boolean shown) {
        shadow = shown;
        applyAll();
    }

    public static int getCoordinateDecimals() {
        return coordinateDecimals;
    }

    public static void setCoordinateDecimals(int decimals) {
        coordinateDecimals = Math.max(0, Math.min(3, decimals));
    }

    public static boolean isTwelveHourClock() {
        return twelveHourClock;
    }

    public static void setTwelveHourClock(boolean twelveHour) {
        twelveHourClock = twelveHour;
    }
}
