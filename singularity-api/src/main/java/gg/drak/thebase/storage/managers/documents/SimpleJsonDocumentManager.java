package gg.drak.thebase.storage.managers.documents;

import gg.drak.thebase.storage.documents.SimpleJsonDocument;

import java.io.File;

public abstract class SimpleJsonDocumentManager extends SimpleDocumentManager<SimpleJsonDocument> {
    public SimpleJsonDocumentManager(File parentDirectory) {
        super(SimpleJsonDocument.class, parentDirectory);
    }
}
