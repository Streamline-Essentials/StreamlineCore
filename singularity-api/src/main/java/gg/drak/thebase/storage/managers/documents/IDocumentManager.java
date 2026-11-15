package gg.drak.thebase.storage.managers.documents;

import gg.drak.thebase.storage.documents.SimpleFlatDocument;
import gg.drak.thebase.storage.managers.IStorageManager;

public interface IDocumentManager<T extends SimpleFlatDocument<?>> extends IStorageManager<T> {
    /**
     * Saves all documents.
     */
    void saveAll();
}
