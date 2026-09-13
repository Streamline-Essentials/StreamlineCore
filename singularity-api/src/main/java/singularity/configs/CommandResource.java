package singularity.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import lombok.Setter;
import singularity.command.CommandHandler;
import singularity.command.CosmicCommand;
import singularity.modules.ModuleLike;
import singularity.modules.CosmicModule;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A per-command YAML configuration file that persists and synchronises the
 * runtime properties of a {@link CosmicCommand} (enabled state, label,
 * permission, and aliases).
 *
 * <p>The file is named after the command's identifier and is written to the
 * supplied parent directory. On construction the file is created (or
 * initialised with defaults if empty/outdated) and then immediately
 * synchronised back to the live command object via {@link #syncCommand()}.</p>
 */
@Getter
public class CommandResource extends SimpleConfiguration {

    /** The command identifier, mirroring {@link CosmicCommand#getIdentifier()}. */
    final String identifier;

    /** The command whose configuration this file represents. */
    @Setter
    CosmicCommand command;

    /** The module that owns this command, if any. */
    @Setter
    ModuleLike module;

    /**
     * Creates a command resource for the given command, placing the YAML file
     * in {@code parentDirectory}. If the file is missing or was created by an
     * older version ({@code DO-NOT-TOUCH.version < 1}), default values are
     * written. The command is synchronised immediately after construction.
     *
     * @param command         the command to persist configuration for
     * @param parentDirectory the directory in which the YAML file will be created
     */
    public CommandResource(CosmicCommand command, File parentDirectory) {
        super(command.getIdentifier() + ".yml", parentDirectory, false);
        this.identifier = command.getIdentifier();
        this.command = command;

        if (this.exists()) {
            if (this.empty()) {
                afterInit();
            }
        } else {
            afterInit();
        }

        if (! getResource().getBoolean("basic.enabled") && getResource().getOrDefault("DO-NOT-TOUCH.version", 0d) < 1d) {
            afterInit();
        }

        syncCommand();
    }

    /**
     * Creates a command resource owned by the given module. Delegates to
     * {@link #CommandResource(CosmicCommand, File)} and then records the
     * owning module.
     *
     * @param module          the module that owns the command
     * @param command         the command to persist configuration for
     * @param parentDirectory the directory in which the YAML file will be created
     */
    public CommandResource(CosmicModule module, CosmicCommand command, File parentDirectory) {
        this(command, parentDirectory);
        this.module = module;
    }

    /**
     * No-op; configuration defaults are applied by {@link #afterInit()} instead.
     */
    @Override
    public void init() {

    }

    /**
     * Writes the versioned default values for this command's configuration,
     * including the format version sentinel, enabled flag, label, default
     * permission, and aliases.
     */
    public void afterInit() {
        write("DO-NOT-TOUCH.version", 1d);
        write("basic.enabled", true);
        write("basic.label", command.getBase());
        write("basic.permissions.default", command.getPermission());
        write("basic.aliases", Arrays.stream(command.getAliases()).collect(Collectors.toList()));
    }

    /**
     * Reads the current configuration values and applies them to the live
     * {@link CosmicCommand} object, registering or unregistering the command
     * with {@link CommandHandler} based on the {@code basic.enabled} flag.
     *
     * <p>If the file predates the versioning scheme ({@code DO-NOT-TOUCH.version < 1})
     * and the command is marked disabled, the enabled flag is force-reset to
     * {@code true} and the version sentinel is written.</p>
     */
    public void syncCommand() {
        boolean enabled = getResource().getBoolean("basic.enabled");

        if (! enabled && getResource().getOrDefault("DO-NOT-TOUCH.version", 0d) < 1d) {
            enabled = true;
            write("basic.enabled", true);
            write("DO-NOT-TOUCH.version", 1d);
        }

        String label = getResource().getString("basic.label");
        String defaultPermission = getResource().getString("basic.permissions.default");
        List<String> aliases = getResource().getStringList("basic.aliases");

        if (this.command.isLoaded()) if (! enabled) CommandHandler.registerStreamlineCommand(this.command);
        if (! this.command.isLoaded()) if (enabled) CommandHandler.unregisterStreamlineCommand(this.command);
        this.command.setBase(label);
        this.command.setPermission(defaultPermission);
        this.command.setAliases(aliases.toArray(new String[0]));
    }
}
