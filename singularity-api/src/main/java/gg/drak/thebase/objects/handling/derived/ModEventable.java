package gg.drak.thebase.objects.handling.derived;

import lombok.Getter;
import gg.drak.thebase.storage.StorageUtils;

import java.io.File;

public abstract class ModEventable implements IModifierEventable {
    @Getter
    final String identifier;
    @Getter
    final ModifierType modifierType = ModifierType.MOD;
    @Getter
    final File dataFolder;

    public ModEventable(String identifier, boolean initDataFolder) {
        this.identifier = identifier;
        this.dataFolder = StorageUtils.initializeModifierEventableFolder(this);

        if (initDataFolder) initializeDataFolder();
    }

    public ModEventable(String identifier) {
        this(identifier, true);
    }

    @Override
    public boolean isPlugin() {
        return getModifierType().equals(ModifierType.PLUGIN);
    }

    @Override
    public boolean isMod() {
        return getModifierType().equals(ModifierType.MOD);
    }

    @Override
    public boolean isStreamline() {
        return getModifierType().equals(ModifierType.STREAMLINE);
    }

    @Override
    public void initializeDataFolder() {
        getDataFolder().mkdirs();
    }
}
