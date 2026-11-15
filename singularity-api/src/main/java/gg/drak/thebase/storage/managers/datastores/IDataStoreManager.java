package gg.drak.thebase.storage.managers.datastores;

import gg.drak.thebase.storage.datastores.SimpleFlatDatastore;
import gg.drak.thebase.storage.managers.IStorageManager;

public interface IDataStoreManager<T extends SimpleFlatDatastore<?, ?>> extends IStorageManager<T> {
    /**
     * Saves all documents.
     */
    void saveAll();
}
