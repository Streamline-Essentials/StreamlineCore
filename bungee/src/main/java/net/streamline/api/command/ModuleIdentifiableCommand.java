package net.streamline.api.command;

import net.streamline.api.modules.Module;
import org.jetbrains.annotations.NotNull;

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
     * @return Plugin that owns this PluginIdentifiableCommand.
     */
    @NotNull
    public Module getModule();
}
