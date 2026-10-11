package net.streamline.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * Builds Streamline's key mappings; this is Minecraft 1.21.11 and later's, where categories
 * are registered {@link KeyMapping.Category} records. The heading's translation key is
 * {@code key.category.streamlinecore.streamlineclient}.
 */
public final class ClientKeys {

    /** Registered once, as this class initializes: registering an id twice throws. */
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("streamlinecore", "streamlineclient"));

    private ClientKeys() {}

    /** @param key an {@link InputConstants} key code, or {@code InputConstants.UNKNOWN}'s for none */
    public static KeyMapping mapping(String name, int key) {
        return new KeyMapping(name, key, CATEGORY);
    }
}
