package host.plas.collections.gui;

import singularity.gui.CornerColor;
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

    /** The shared shell: a black border with corners in the pane colour closest to {@code accent}. */
    public static void shell(CosmicGui gui, String accent) {
        gui.shell(CornerColor.fromDisplayText(accent));
    }

    public static GuiIcon back(Runnable action) {
        return GuiIcon.of(CosmicItem.of("minecraft:arrow", "#00FC88ʙᴀᴄᴋ", "#FFFFFFClick to go back"), click -> action.run());
    }

    public static GuiIcon close() {
        return GuiIcon.close(CosmicItem.of("minecraft:barrier", "&c&lClose", "&7Close this GUI."));
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
        if (count % 2 == 0 && count < 9) {
            // An even count leaves the middle column free, half on each side of it.
            int half = count / 2;
            for (int i = 0; i < half; i++) slots[i] = GuiLayout.slot(row, 4 - half + i);
            for (int i = 0; i < half; i++) slots[half + i] = GuiLayout.slot(row, 5 + i);
            return slots;
        }
        int start = (9 - count) / 2;
        for (int i = 0; i < count; i++) slots[i] = GuiLayout.slot(row, start + i);
        return slots;
    }

    public static List<Integer> contentSlots() {
        return GuiLayout.innerSlots(ROWS);
    }
}
