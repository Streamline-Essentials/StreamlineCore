package net.streamline.api.configs;

import de.leonhard.storage.Config;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;

import java.io.File;

public class ModularizedConfig extends FlatFileResource<Config> {
    public ModularizedConfig(ModuleLike module, String fileName, File parentDirectory, boolean selfContained) {
        super(module, Config.class, fileName, parentDirectory, selfContained);
    }

    public ModularizedConfig(ModuleLike module, String fileName, boolean selfContained) {
        this(module, fileName, module.getDataFolder(), selfContained);
    }
}
