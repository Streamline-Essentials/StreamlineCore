package gg.drak.thebase.storage.datastores;

import de.leonhard.storage.Toml;
import gg.drak.thebase.objects.handling.derived.IModifierEventable;

import java.io.File;

public abstract class SimpleTomlDatastore<T> extends SimpleFlatDatastore<Toml, T> {
    public SimpleTomlDatastore(String fileName, File parentDirectory, boolean selfContained) {
        super(Toml.class, fileName, parentDirectory, selfContained);
    }

    public SimpleTomlDatastore(String fileName, IModifierEventable eventable, boolean selfContained) {
        super(Toml.class, fileName, eventable, selfContained);
    }

    public SimpleTomlDatastore(String fileName, File parentDirectory) {
        super(Toml.class, fileName, parentDirectory);
    }

    public SimpleTomlDatastore(String fileName, IModifierEventable eventable) {
        super(Toml.class, fileName, eventable);
    }
}
