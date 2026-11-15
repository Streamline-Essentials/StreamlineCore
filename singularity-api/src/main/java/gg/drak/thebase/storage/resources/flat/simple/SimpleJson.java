package gg.drak.thebase.storage.resources.flat.simple;

import de.leonhard.storage.Json;
import gg.drak.thebase.objects.handling.derived.IModifierEventable;
import gg.drak.thebase.storage.resources.flat.FlatFileResource;

import java.io.File;

public abstract class SimpleJson extends FlatFileResource<Json> {
    public SimpleJson(String fileName, File parentDirectory, boolean selfContained) {
        super(Json.class, fileName, parentDirectory, selfContained);
    }

    public SimpleJson(String fileName, IModifierEventable eventable, boolean selfContained) {
        this(fileName, eventable.getDataFolder(), selfContained);
    }

    public SimpleJson(String fileName, File parentDirectory) {
        this(fileName, parentDirectory, false);
    }

    public SimpleJson(String fileName, IModifierEventable eventable) {
        this(fileName, eventable, false);
    }
}
