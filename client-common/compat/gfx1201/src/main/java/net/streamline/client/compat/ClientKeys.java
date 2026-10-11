package net.streamline.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * Builds Streamline's key mappings; this is Minecraft 1.20 and 1.21.1's, where a key
 * mapping's category is the translation key of its heading in the Controls screen.
 */
public final class ClientKeys {

    private static final String CATEGORY = "key.category.streamlinecore.streamlineclient";

    private ClientKeys() {}

    /** @param key an {@link InputConstants} key code, or {@code InputConstants.UNKNOWN}'s for none */
    public static KeyMapping mapping(String name, int key) {
        return new KeyMapping(name, key, CATEGORY);
    }
}
