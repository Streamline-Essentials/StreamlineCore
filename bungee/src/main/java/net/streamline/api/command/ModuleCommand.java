package net.streamline.api.command;

import com.google.common.base.Preconditions;
import net.streamline.api.modules.Module;
import net.streamline.base.Streamline;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a {@link Command} belonging to a module
 */
public abstract class ModuleCommand extends StreamlineCommand {
    private final Module owningModule;
    private CommandExecutor executor;
    private TabCompleter completer;

    public ModuleCommand(Module module, String base, String description, String usageMessage, String permission, String... aliases) {
        super(module, base, description, usageMessage, permission, aliases);
        this.owningModule = module;
    }
}