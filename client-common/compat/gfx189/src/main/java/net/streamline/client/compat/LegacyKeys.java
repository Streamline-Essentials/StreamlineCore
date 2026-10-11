package net.streamline.client.compat;

import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Translates between modern Minecraft's key names ({@code key.keyboard.g}), which client
 * modules save, and LWJGL 2's key codes, which 1.8.9 reads. Letters, digits and function
 * keys share their names; the rest are listed.
 */
public final class LegacyKeys {

    private static final String PREFIX = "key.keyboard.";
    private static final Map<String, Integer> CODES = new HashMap<String, Integer>();
    private static final Map<Integer, String> NAMES = new HashMap<Integer, String>();

    static {
        add("space", Keyboard.KEY_SPACE);
        add("left.shift", Keyboard.KEY_LSHIFT);
        add("right.shift", Keyboard.KEY_RSHIFT);
        add("left.control", Keyboard.KEY_LCONTROL);
        add("right.control", Keyboard.KEY_RCONTROL);
        add("left.alt", Keyboard.KEY_LMENU);
        add("right.alt", Keyboard.KEY_RMENU);
        add("tab", Keyboard.KEY_TAB);
        add("caps.lock", Keyboard.KEY_CAPITAL);
        add("grave.accent", Keyboard.KEY_GRAVE);
        add("minus", Keyboard.KEY_MINUS);
        add("equal", Keyboard.KEY_EQUALS);
        add("left.bracket", Keyboard.KEY_LBRACKET);
        add("right.bracket", Keyboard.KEY_RBRACKET);
        add("backslash", Keyboard.KEY_BACKSLASH);
        add("semicolon", Keyboard.KEY_SEMICOLON);
        add("apostrophe", Keyboard.KEY_APOSTROPHE);
        add("comma", Keyboard.KEY_COMMA);
        add("period", Keyboard.KEY_PERIOD);
        add("slash", Keyboard.KEY_SLASH);
        add("enter", Keyboard.KEY_RETURN);
        add("escape", Keyboard.KEY_ESCAPE);
        add("backspace", Keyboard.KEY_BACK);
        add("insert", Keyboard.KEY_INSERT);
        add("delete", Keyboard.KEY_DELETE);
        add("home", Keyboard.KEY_HOME);
        add("end", Keyboard.KEY_END);
        add("page.up", Keyboard.KEY_PRIOR);
        add("page.down", Keyboard.KEY_NEXT);
        add("up", Keyboard.KEY_UP);
        add("down", Keyboard.KEY_DOWN);
        add("left", Keyboard.KEY_LEFT);
        add("right", Keyboard.KEY_RIGHT);
        add("keypad.add", Keyboard.KEY_ADD);
        add("keypad.subtract", Keyboard.KEY_SUBTRACT);
        add("keypad.multiply", Keyboard.KEY_MULTIPLY);
        add("keypad.divide", Keyboard.KEY_DIVIDE);
        add("keypad.decimal", Keyboard.KEY_DECIMAL);
        add("keypad.enter", Keyboard.KEY_NUMPADENTER);
        int[] keypad = {Keyboard.KEY_NUMPAD0, Keyboard.KEY_NUMPAD1, Keyboard.KEY_NUMPAD2, Keyboard.KEY_NUMPAD3,
                Keyboard.KEY_NUMPAD4, Keyboard.KEY_NUMPAD5, Keyboard.KEY_NUMPAD6, Keyboard.KEY_NUMPAD7,
                Keyboard.KEY_NUMPAD8, Keyboard.KEY_NUMPAD9};
        for (int i = 0; i < keypad.length; i++) add("keypad." + i, keypad[i]);
    }

    private LegacyKeys() {}

    private static void add(String suffix, int code) {
        CODES.put(PREFIX + suffix, code);
        NAMES.put(code, PREFIX + suffix);
    }

    /** @return the LWJGL 2 code of the key named {@code name}, or {@link Keyboard#KEY_NONE} */
    public static int code(String name) {
        if (name == null || ! name.startsWith(PREFIX)) return Keyboard.KEY_NONE;
        Integer listed = CODES.get(name);
        if (listed != null) return listed;
        String suffix = name.substring(PREFIX.length());
        // Letters, digits and F1..F19 are named alike on both sides.
        if (suffix.length() == 1 || suffix.matches("f[0-9]{1,2}")) {
            return Keyboard.getKeyIndex(suffix.toUpperCase(Locale.ROOT));
        }
        return Keyboard.KEY_NONE;
    }

    /** @return the modern name of the key with LWJGL 2 code {@code code}, or {@code null} for one not listed */
    public static String name(int code) {
        if (code <= Keyboard.KEY_NONE) return null;
        String listed = NAMES.get(code);
        if (listed != null) return listed;
        String lwjgl = Keyboard.getKeyName(code);
        if (lwjgl == null) return null;
        String suffix = lwjgl.toLowerCase(Locale.ROOT);
        if (suffix.length() == 1 && Character.isLetterOrDigit(suffix.charAt(0))) return PREFIX + suffix;
        if (suffix.matches("f[0-9]{1,2}")) return PREFIX + suffix;
        return null;
    }
}
