package gg.drak.thebase.storage.documents;

import de.leonhard.storage.internal.FlatFile;
import gg.drak.thebase.objects.handling.derived.IModifierEventable;
import gg.drak.thebase.storage.resources.flat.FlatFileResource;

import java.io.File;

public abstract class SimpleFlatDocument<T extends FlatFile> extends FlatFileResource<T> implements ISimpleDocument {
    public SimpleFlatDocument(Class<T> resourceType, String fileName, File parentDirectory, boolean selfContained) {
        super(resourceType, fileName, parentDirectory, selfContained);
        init();
    }

    public SimpleFlatDocument(Class<T> resourceType, String fileName, IModifierEventable eventable, boolean selfContained) {
        this(resourceType, fileName, eventable.getDataFolder(), selfContained);
        init();
    }

    public SimpleFlatDocument(Class<T> resourceType, String fileName, File parentDirectory) {
        this(resourceType, fileName, parentDirectory, false);
        init();
    }

    public SimpleFlatDocument(Class<T> resourceType, String fileName, IModifierEventable eventable) {
        this(resourceType, fileName, eventable, false);
        init();
    }

    @Override
    public void init() {
        onInit();
    }

    public abstract void onInit();

    @Override
    public void save() {
        getResource().write();
    }

    public abstract void onSave();

    @Override
    public void delete() {
        getSelfFile().delete();
    }

    @Override
    public boolean exists() {
        return getSelfFile().exists();
    }
}
