package net.streamline.api.base.modules;

/**
 * Represents the order in which a module should be initialized and enabled
 */
public enum ModuleLoadOrder {

    /**
     * Indicates that the module will be loaded at startup
     */
    STARTUP,
    /**
     * Indicates that the module will be loaded after the first/default world
     * was created
     */
    POSTWORLD
}
