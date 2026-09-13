package net.streamline.api.holders;

/**
 * Bootstrap utility that registers all compatibility dependency holders
 * supported by the Streamline API layer. Call {@link #init()} once during
 * plugin start-up to ensure every integration (e.g., Geyser/Floodgate) is
 * set up before any code tries to query it.
 */
public class HolderCompat {

    /**
     * Instantiates and registers all known dependency-holder initialisers.
     * Currently registers the Geyser/Floodgate integration via
     * {@link GeyserInit}.
     */
    public static void init() {
        new GeyserInit();
    }
}
