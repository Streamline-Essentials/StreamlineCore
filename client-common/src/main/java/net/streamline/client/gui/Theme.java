package net.streamline.client.gui;

import net.streamline.client.Canvas;

import java.util.ArrayList;
import java.util.List;

/**
 * Colors and the few shapes Streamline's screens draw themselves, so the menu and the HUD
 * editor look alike on every Minecraft version without depending on vanilla widgets.
 */
final class Theme {

    // Streamline red (#D32F2F) leads; buttons and controls use nearby shades of it.
    static final int BACKDROP = 0x90000000;
    static final int PANEL = 0xF0181415;
    static final int HEADER = 0xFFD32F2F;
    static final int SIDEBAR = 0xFF1F1718;
    static final int ACCENT = 0xFFD32F2F;
    static final int ACCENT_LIGHT = 0xFFE57373;
    static final int BUTTON = 0xFFB02424;
    static final int BUTTON_HOVER = 0xFFD84343;
    static final int BUTTON_DARK = 0xFF5E1717;
    static final int BUTTON_DARK_HOVER = 0xFF7A2020;
    static final int TEXT = 0xFFE6E6E6;
    static final int MUTED = 0xFFA89A9B;
    static final int HOVER = 0x18FFFFFF;
    /** Behind selected text in a field. */
    static final int SELECTION = 0x90D32F2F;
    static final int CONTROL = 0xFF2A1F20;
    static final int CONTROL_BORDER = 0xFF4D2A2D;
    static final int SWITCH_OFF = 0xFF4A3B3D;
    /** Behind a tag pill: blue, so a label never reads as one of the red buttons. */
    static final int TAG = 0xFF2F6FD3;

    static final int GLFW_ESCAPE = 256;
    static final int GLFW_ENTER = 257;
    static final int GLFW_BACKSPACE = 259;
    static final int GLFW_KP_ENTER = 335;
    static final int GLFW_DELETE = 261;
    static final int GLFW_RIGHT = 262;
    static final int GLFW_LEFT = 263;
    static final int GLFW_DOWN = 264;
    static final int GLFW_UP = 265;
    static final int GLFW_HOME = 268;
    static final int GLFW_END = 269;
    static final int GLFW_R = 82;
    static final int GLFW_A = 65;
    static final int GLFW_C = 67;
    static final int GLFW_V = 86;
    static final int GLFW_X = 88;
    static final int GLFW_MOD_SHIFT = 0x1;
    /** Control, or Command on macOS, as GLFW modifier bits. */
    static final int GLFW_MOD_SHORTCUT = 0x2 | 0x8;

    private Theme() {}

    static boolean inside(double x, double y, int x1, int y1, int x2, int y2) {
        return x >= x1 && y >= y1 && x < x2 && y < y2;
    }

    /** A flat button with centred text: primary ones bright red, others dark red; hovered ones lighten. */
    static void button(Canvas canvas, int x1, int y1, int x2, int y2, String label, boolean hovered, boolean primary) {
        int color = primary ? (hovered ? BUTTON_HOVER : BUTTON) : (hovered ? BUTTON_DARK_HOVER : BUTTON_DARK);
        canvas.fill(x1, y1, x2, y2, color);
        int textX = x1 + (x2 - x1 - canvas.textWidth(label)) / 2;
        int textY = y1 + (y2 - y1 - canvas.lineHeight()) / 2 + 1;
        canvas.text(label, textX, textY, TEXT);
    }

    /**
     * @return the width of {@link #tag} for {@code label}: the label with a 2-pixel left side and a
     *         1-pixel right one, since the font's advance already ends each glyph with a blank column
     */
    static int tagWidth(Canvas canvas, String label) {
        return canvas.textWidth(label) + 3;
    }

    /**
     * A tag pill: {@code label} on a blue rectangle hugging the text, with its text on the
     * line at {@code textY} so it sits inline with text there. A label only; it takes no clicks.
     *
     * @return the x right after the pill
     */
    static int tag(Canvas canvas, int x, int textY, String label) {
        int x2 = x + tagWidth(canvas, label);
        canvas.fill(x, textY - 2, x2, textY + canvas.lineHeight() + 1, TAG);
        canvas.text(label, x + 2, textY, TEXT);
        return x2;
    }

    /** An on/off switch, 20 by 10 pixels. */
    static void toggle(Canvas canvas, int x, int y, boolean on, boolean hovered) {
        canvas.fill(x, y, x + 20, y + 10, on ? ACCENT : SWITCH_OFF);
        if (hovered) canvas.outline(x, y, x + 20, y + 10, 0x60FFFFFF);
        int knob = on ? x + 11 : x + 1;
        canvas.fill(knob, y + 1, knob + 8, y + 9, 0xFFF0F0F0);
    }

    /** Splits plain text into lines no wider than {@code width}, breaking between words. */
    static List<String> wrap(Canvas canvas, String text, int width) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && canvas.textWidth(candidate) > width) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    /** Shortens text with an ellipsis so it fits {@code width}. */
    static String fit(Canvas canvas, String text, int width) {
        if (canvas.textWidth(text) <= width) return text;
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && canvas.textWidth(text.substring(0, end) + ellipsis) > width) end--;
        return text.substring(0, end) + ellipsis;
    }
}
