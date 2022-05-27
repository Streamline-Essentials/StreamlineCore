package net.streamline.api.base.command;

import net.streamline.api.base.modules.Module;

/**
 * This interface is used by the help system to group commands into
 * sub-indexes based on the {@link Module} they are a part of. Custom command
 * implementations will need to implement this interface to have a sub-index
 * automatically generated on the plugin's behalf.
 */
public interface ModuleIdentifiableCommand {
    /**
     * Gets the owner of this PluginIdentifiableCommand.
     *
     * @return Module that owns this PluginIdentifiableCommand.
     */
    public Module getPlugin();
}
