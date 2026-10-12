package singularity.command;

import lombok.Getter;
import singularity.Singularity;
import singularity.modules.ModuleLike;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A {@link CosmicCommand} that is owned by a PF4J module ({@link ModuleLike}).
 * In addition to the standard command lifecycle, module commands are tracked inside
 * the owning module via {@link #modulize()} and {@link #demodulize()}.
 */
@Getter
public abstract class ModuleCommand extends CosmicCommand {

    /**
     * The PF4J module that owns and manages this command.
     */
    private final ModuleLike owningModule;

    /**
     * Creates a {@code ModuleCommand} whose configuration file is stored inside the
     * module's data folder under the standard commands sub-folder.
     *
     * @param module     the owning module
     * @param base       the primary command name
     * @param permission the permission node required to execute the command
     * @param aliases    alternative names for this command
     */
    public ModuleCommand(ModuleLike module, String base, String permission, String... aliases) {
        this(module, base, permission, new File(module.getDataFolder(), Singularity.getCommandsFolderChild()), aliases);
    }

    /**
     * Creates a {@code ModuleCommand} with a custom parent directory for its
     * configuration file.
     *
     * @param module          the owning module
     * @param base            the primary command name
     * @param permission      the permission node required to execute the command
     * @param parentDirectory the directory in which the command's config file is stored
     * @param aliases         alternative names for this command
     */
    public ModuleCommand(ModuleLike module, String base, String permission, File parentDirectory, String... aliases) {
        super(module.getIdentifier(), base, permission, parentDirectory, aliases);
        this.owningModule = module;
    }

    /**
     * {@code <module-id>:<label>} for the base and every alias, so a module's command stays
     * reachable as {@code /<module-id>:<command>} when another plugin or module takes the bare
     * name. Built from the live base and aliases, so labels renamed in the command's config
     * file are namespaced too.
     */
    @Override
    public List<String> getNamespacedLabels() {
        List<String> labels = new ArrayList<>();
        String namespace = getOwningModule().getIdentifier();
        if (namespace == null || namespace.isEmpty()) return labels;
        namespace = namespace.toLowerCase(Locale.ROOT);

        List<String> bare = new ArrayList<>();
        bare.add(getBase());
        if (getAliases() != null) bare.addAll(Arrays.asList(getAliases()));
        for (String label : bare) {
            if (label == null || label.isEmpty() || label.contains(":")) continue;
            String namespaced = namespace + ":" + label.toLowerCase(Locale.ROOT);
            if (! labels.contains(namespaced)) labels.add(namespaced);
        }
        return labels;
    }

    /**
     * Adds this command to the owning module's internal command list, enabling the
     * module to track and manage it during its lifecycle.
     */
    public void modulize() {
        getOwningModule().addCommand(this);
    }

    /**
     * Removes this command from the owning module's internal command list.
     */
    public void demodulize() {
        getOwningModule().removeCommand(this);
    }

    /**
     * {@inheritDoc}
     *
     * Additionally calls {@link #modulize()} to register this command with its owning
     * module after it is registered with the platform.
     */
    @Override
    public void register() {
        if (! isEnabled()) return;

        CommandHandler.registerModuleCommand(this);

        modulize();
    }

    /**
     * {@inheritDoc}
     *
     * Additionally calls {@link #demodulize()} to remove this command from its owning
     * module after it is unregistered from the platform.
     */
    @Override
    public void unregister() {
        if (! isEnabled()) if (! CommandHandler.getLoadedModuleCommands().containsKey(getIdentifier())) return;

        CommandHandler.unregisterModuleCommand(this);

        demodulize();
    }

    /**
     * {@inheritDoc}
     *
     * Unregisters this command from the module command registry rather than the
     * Streamline command registry.
     */
    @Override
    public void disable() {
        CommandHandler.unregisterModuleCommand(this);
    }
}