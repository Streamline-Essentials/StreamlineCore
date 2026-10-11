package host.plas.toggles;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The placeholders this module's HUD element text understands, written the RAT way:
 * {@code %clienttogglesmodule_<sprinting|sneaking>_<toggled|text>%}. Game clients run without
 * Streamline's RAT registry, so {@link ToggleElement} fills them in itself.
 */
public final class Placeholders {

    public static final String IDENTIFIER = "clienttogglesmodule";
    /** RAT's line break, honoured in element text. */
    public static final String NEWLINE = "%newline%";

    public static final String SPRINTING_TOGGLED = token("sprinting_toggled");
    public static final String SPRINTING_TEXT = token("sprinting_text");
    public static final String SNEAKING_TOGGLED = token("sneaking_toggled");
    public static final String SNEAKING_TEXT = token("sneaking_text");

    private static final Map<String, String> DESCRIPTIONS = new LinkedHashMap<>();

    static {
        DESCRIPTIONS.put(SPRINTING_TOGGLED, "Yes or No: whether sprint is toggled");
        DESCRIPTIONS.put(SPRINTING_TEXT, "Sprinting Toggled / Sprinting Not Toggled");
        DESCRIPTIONS.put(SNEAKING_TOGGLED, "Yes or No: whether sneak is toggled");
        DESCRIPTIONS.put(SNEAKING_TEXT, "Sneaking Toggled / Sneaking Not Toggled");
        DESCRIPTIONS.put(NEWLINE, "starts a new line");
    }

    private Placeholders() {}

    public static String token(String key) {
        return "%" + IDENTIFIER + "_" + key + "%";
    }

    /** @return {@code text} with every placeholder filled in from the current toggles */
    public static String apply(String text) {
        if (text.indexOf('%') < 0) return text;
        return text
                .replace(SPRINTING_TOGGLED, Toggles.yesNo(Toggles.isSprinting()))
                .replace(SPRINTING_TEXT, Toggles.sprintingText())
                .replace(SNEAKING_TOGGLED, Toggles.yesNo(Toggles.isSneaking()))
                .replace(SNEAKING_TEXT, Toggles.sneakingText());
    }

    /** @return every token with what it shows, for the menu */
    public static Map<String, String> descriptions() {
        return Collections.unmodifiableMap(DESCRIPTIONS);
    }
}
