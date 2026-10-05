package host.plas.collections.gui;

import singularity.gui.CosmicGui;
import singularity.gui.CosmicItem;
import singularity.gui.GuiIcon;
import singularity.gui.GuiLayout;

import java.util.List;
import java.util.Locale;

/**
 * Shared pieces of the collections menus: the bordered shell, navigation buttons and number
 * formatting.
 */
public final class Menus {
    private Menus() {
    }

    public static final int ROWS = 6;
    public static final int BACK_SLOT = GuiLayout.bottomCenter(ROWS);

    private static final String[] PANES = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
    };
    private static final int[] PANE_RGB = {
            0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
            0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21,
    };

    /** A blank-named pane, used for borders. */
    public static CosmicItem pane(String color) {
        return CosmicItem.of("minecraft:" + color + "_stained_glass_pane", " ");
    }

    /** The stained glass pane colour closest to {@code hex} ({@code #RRGGBB}). */
    public static String closestPane(String hex) {
        int rgb;
        try {
            rgb = Integer.parseInt(hex.replace("#", "").trim(), 16);
        } catch (Exception e) {
            return "yellow";
        }
        int best = 4;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < PANE_RGB.length; i++) {
            long dr = ((rgb >> 16) & 0xFF) - ((PANE_RGB[i] >> 16) & 0xFF);
            long dg = ((rgb >> 8) & 0xFF) - ((PANE_RGB[i] >> 8) & 0xFF);
            long db = (rgb & 0xFF) - (PANE_RGB[i] & 0xFF);
            long distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return PANES[best];
    }

    /** A black border with corners tinted to {@code accent}. */
    public static void shell(CosmicGui gui, String accent) {
        gui.border(pane("black"));
        CosmicItem corner = pane(closestPane(accent));
        for (int slot : GuiLayout.cornerSlots(gui.getRows())) gui.setItem(slot, corner);
    }

    public static GuiIcon back(Runnable action) {
        return GuiIcon.of(CosmicItem.of("minecraft:arrow", "#00FC88Back", "#FFFFFFClick to go back"), click -> action.run());
    }

    public static GuiIcon close() {
        return GuiIcon.close(CosmicItem.of("minecraft:barrier", "&c&lClose", "&7Close this menu."));
    }

    public static String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    public static long percent(long part, long total) {
        return total <= 0 ? 0 : Math.round(part * 100.0 / total);
    }

    private static final String[] ROMAN = {"0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    public static String roman(int value) {
        return value >= 0 && value < ROMAN.length ? ROMAN[value] : String.valueOf(value);
    }

    /** {@code count} slots centred on row {@code row}, for up to nine items. */
    public static int[] centredRow(int row, int count) {
        count = Math.max(0, Math.min(9, count));
        int[] slots = new int[count];
        int start = (9 - count) / 2;
        for (int i = 0; i < count; i++) slots[i] = GuiLayout.slot(row, start + i);
        return slots;
    }

    public static List<Integer> contentSlots() {
        return GuiLayout.innerSlots(ROWS);
    }
}
