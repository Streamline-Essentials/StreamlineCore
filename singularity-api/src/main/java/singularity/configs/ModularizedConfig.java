package singularity.configs;

import lombok.Getter;
import singularity.modules.ModuleLike;
import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;

import java.io.File;

@Getter
public abstract class ModularizedConfig extends SimpleConfiguration {
    final ModuleLike module;

    public ModularizedConfig(ModuleLike module, String fileName, File parentDirectory, boolean selfContained) {
        super(fileName, parentDirectory, selfContained);
        this.module = module;
    }

    public ModularizedConfig(ModuleLike module, String fileName, boolean selfContained) {
        this(module, fileName, module.getDataFolder(), selfContained);
    }
}
