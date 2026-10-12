package net.streamline.client;

import net.minecraft.network.chat.Component;
import net.streamline.platform.text.LegacyText;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns the color-coded strings {@link Canvas} takes into Minecraft text, through
 * {@link LegacyText}. Parsed strings are cached, since most GUI and HUD text repeats from
 * frame to frame.
 */
public final class ModernText {

    private static final int CACHE_LIMIT = 512;
    private static final Map<String, Component> PARSED = new ConcurrentHashMap<>();

    private ModernText() {}

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        Component cached = PARSED.get(text);
        if (cached != null) return cached;
        if (PARSED.size() >= CACHE_LIMIT) PARSED.clear();
        Component parsed = LegacyText.parse(text);
        PARSED.put(text, parsed);
        return parsed;
    }
}
