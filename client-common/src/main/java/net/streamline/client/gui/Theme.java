package net.streamline.client.gui;

import net.streamline.client.Canvas;

import java.util.ArrayList;
import java.util.List;

/**
 * Colors and the few shapes Streamline's screens draw themselves, so the menu and the HUD
 * editor look alike on every Minecraft version without depending on vanilla widgets.
 */
final class Theme {

    static final int BACKDROP = 0x90000000;
    static final int PANEL = 0xF0181A1F;
    static final int HEADER = 0xFF22252B;
    static final int SIDEBAR = 0xFF1D2025;
    static final int ACCENT = 0xFF4F8CFF;
    static final int ACCENT_DIM = 0xFF2E4F8F;
    static final int TEXT = 0xFFE6E6E6;
    static final int MUTED = 0xFF9AA0A6;
    static final int HOVER = 0x18FFFFFF;
    static final int CONTROL = 0xFF2B2F36;
    static final int CONTROL_BORDER = 0xFF3A3F47;
    static final int SWITCH_OFF = 0xFF4A4F57;
    static final int WARNING = 0xFFE0A040;

    static final int GLFW_ESCAPE = 256;
    static final int GLFW_ENTER = 257;
    static final int GLFW_BACKSPACE = 259;
    static final int GLFW_KP_ENTER = 335;
    static final int GLFW_R = 82;

    private Theme() {}

    static boolean inside(double x, double y, int x1, int y1, int x2, int y2) {
        return x >= x1 && y >= y1 && x < x2 && y < y2;
    }

    /** A flat button with centred text; hovered buttons light up. */
    static void button(Canvas canvas, int x1, int y1, int x2, int y2, String label, boolean hovered, boolean primary) {
        int color = primary ? (hovered ? ACCENT : ACCENT_DIM) : (hovered ? CONTROL_BORDER : CONTROL);
        canvas.fill(x1, y1, x2, y2, color);
        int textX = x1 + (x2 - x1 - canvas.textWidth(label)) / 2;
        int textY = y1 + (y2 - y1 - canvas.lineHeight()) / 2 + 1;
        canvas.text(label, textX, textY, TEXT);
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
