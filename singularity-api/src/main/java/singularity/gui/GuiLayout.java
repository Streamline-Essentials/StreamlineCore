package singularity.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Slot arithmetic for chest-style GUIs (9 columns, 1 to 6 rows).
 */
public final class GuiLayout {
    private GuiLayout() {
    }

    public static int slot(int row, int column) {
        return row * 9 + column;
    }

    /** Every slot of the outer ring. */
    public static List<Integer> borderSlots(int rows) {
        rows = CosmicGui.clampRows(rows);
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < rows * 9; i++) {
            int row = i / 9;
            int column = i % 9;
            if (row == 0 || row == rows - 1 || column == 0 || column == 8) slots.add(i);
        }
        return slots;
    }

    /** The four corner slots. */
    public static List<Integer> cornerSlots(int rows) {
        rows = CosmicGui.clampRows(rows);
        List<Integer> slots = new ArrayList<>();
        slots.add(0);
        slots.add(8);
        if (rows > 1) {
            slots.add((rows - 1) * 9);
            slots.add(rows * 9 - 1);
        }
        return slots;
    }

    /**
     * The slots inside a rectangle that leaves {@code padTop} rows, {@code padBottom} rows,
     * {@code padLeft} columns and {@code padRight} columns free, row by row.
     */
    public static List<Integer> paddedSlots(int rows, int padTop, int padBottom, int padLeft, int padRight) {
        rows = CosmicGui.clampRows(rows);
        List<Integer> slots = new ArrayList<>();
        for (int row = padTop; row < rows - padBottom; row++) {
            for (int column = padLeft; column < 9 - padRight; column++) {
                slots.add(slot(row, column));
            }
        }
        return slots;
    }

    /** The slots inside the border: rows 1 to rows-2, columns 1 to 7. */
    public static List<Integer> innerSlots(int rows) {
        return paddedSlots(rows, 1, 1, 1, 1);
    }

    /** The middle slot of the bottom row. */
    public static int bottomCenter(int rows) {
        return slot(CosmicGui.clampRows(rows) - 1, 4);
    }
}
