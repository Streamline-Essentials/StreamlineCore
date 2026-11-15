package gg.drak.thebase.storage.documents;

import de.leonhard.storage.Json;
import gg.drak.thebase.objects.handling.derived.IModifierEventable;

import java.io.File;

public abstract class SimpleJsonDocument extends SimpleFlatDocument<Json> {
    public SimpleJsonDocument(String fileName, File parentDirectory, boolean selfContained) {
        super(Json.class, fileName, parentDirectory, selfContained);
    }

    public SimpleJsonDocument(String fileName, IModifierEventable eventable, boolean selfContained) {
        super(Json.class, fileName, eventable.getDataFolder(), selfContained);
    }

    public SimpleJsonDocument(String fileName, File parentDirectory) {
        super(Json.class, fileName, parentDirectory, false);
    }

    public SimpleJsonDocument(String fileName, IModifierEventable eventable) {
        super(Json.class, fileName, eventable, false);
    }
}
