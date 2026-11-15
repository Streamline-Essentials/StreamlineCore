package gg.drak.thebase.storage.datastores;

import de.leonhard.storage.internal.FlatFile;
import gg.drak.thebase.objects.handling.derived.IModifierEventable;
import gg.drak.thebase.storage.documents.SimpleFlatDocument;

import java.io.File;

public abstract class SimpleFlatDatastore<F extends FlatFile, T> extends SimpleFlatDocument<F> implements ISimpleDatastore<T> {
    public SimpleFlatDatastore(Class<F> resourceType, String fileName, File parentDirectory, boolean selfContained) {
        super(resourceType, fileName, parentDirectory, selfContained);
    }

    public SimpleFlatDatastore(Class<F> resourceType, String fileName, IModifierEventable eventable, boolean selfContained) {
        super(resourceType, fileName, eventable, selfContained);
    }

    public SimpleFlatDatastore(Class<F> resourceType, String fileName, File parentDirectory) {
        super(resourceType, fileName, parentDirectory);
    }

    public SimpleFlatDatastore(Class<F> resourceType, String fileName, IModifierEventable eventable) {
        super(resourceType, fileName, eventable);
    }

    @Override
    public void delete(String key) {
        getResource().remove(key);
    }

    @Override
    public boolean exists(String key) {
        return getResource().contains(key);
    }
}
