package gg.drak.thebase.storage.resources.flat.simple;

import de.leonhard.storage.Toml;
import gg.drak.thebase.objects.handling.derived.IModifierEventable;
import gg.drak.thebase.storage.resources.flat.FlatFileResource;

import java.io.File;

public abstract class SimpleToml extends FlatFileResource<Toml> {
    public SimpleToml(String fileName, File parentDirectory, boolean selfContained) {
        super(Toml.class, fileName, parentDirectory, selfContained);
    }

    public SimpleToml(String fileName, IModifierEventable eventable, boolean selfContained) {
        this(fileName, eventable.getDataFolder(), selfContained);
    }

    public SimpleToml(String fileName, File parentDirectory) {
        this(fileName, parentDirectory, false);
    }

    public SimpleToml(String fileName, IModifierEventable eventable) {
        this(fileName, eventable, false);
    }
}
