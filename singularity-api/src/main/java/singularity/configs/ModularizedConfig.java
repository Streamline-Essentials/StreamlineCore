package singularity.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import singularity.modules.ModuleLike;

import java.io.File;

/**
 * A {@link SimpleConfiguration} that is scoped to a specific {@link ModuleLike},
 * automatically placing the configuration file inside the module's own data folder
 * unless an explicit parent directory is provided.
 */
@Getter
public abstract class ModularizedConfig extends SimpleConfiguration {

    /** The module that owns this configuration file. */
    final ModuleLike module;

    /**
     * Creates a modularized configuration backed by the given file in the specified
     * parent directory.
     *
     * @param module          the owning module
     * @param fileName        the name of the configuration file (e.g. {@code config.yml})
     * @param parentDirectory the directory in which the file resides
     * @param selfContained   {@code true} if the default file should be loaded from the
     *                        module's bundled resources; {@code false} to create an
     *                        empty file when missing
     */
    public ModularizedConfig(ModuleLike module, String fileName, File parentDirectory, boolean selfContained) {
        super(fileName, parentDirectory, selfContained);
        this.module = module;
    }

    /**
     * Creates a modularized configuration in the module's own data folder.
     * Delegates to {@link #ModularizedConfig(ModuleLike, String, File, boolean)}
     * using {@link ModuleLike#getDataFolder()} as the parent directory.
     *
     * @param module        the owning module
     * @param fileName      the name of the configuration file
     * @param selfContained {@code true} if the default file should be loaded from
     *                      the module's bundled resources
     */
    public ModularizedConfig(ModuleLike module, String fileName, boolean selfContained) {
        this(module, fileName, module.getDataFolder(), selfContained);
    }
}
