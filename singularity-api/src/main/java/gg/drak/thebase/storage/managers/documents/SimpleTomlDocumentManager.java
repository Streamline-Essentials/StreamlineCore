package gg.drak.thebase.storage.managers.documents;

import gg.drak.thebase.storage.documents.SimpleTomlDocument;

import java.io.File;

public abstract class SimpleTomlDocumentManager extends SimpleDocumentManager<SimpleTomlDocument> {
    public SimpleTomlDocumentManager(File parentDirectory) {
        super(SimpleTomlDocument.class, parentDirectory);
    }
}
