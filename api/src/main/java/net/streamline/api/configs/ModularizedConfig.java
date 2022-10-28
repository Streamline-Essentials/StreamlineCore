package net.streamline.api.configs;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;
import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;

import java.io.File;

public abstract class ModularizedConfig extends SimpleConfiguration {
    @Getter
    final ModuleLike module;

    public ModularizedConfig(ModuleLike module, String fileName, File parentDirectory, boolean selfContained) {
        super(fileName, parentDirectory, selfContained);
        this.module = module;
    }

    public ModularizedConfig(ModuleLike module, String fileName, boolean selfContained) {
        this(module, fileName, module.getDataFolder(), selfContained);
    }
}
