package host.plas.collections.database;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.CollectionPlayer;
import singularity.database.modules.DBKeeper;
import singularity.loading.Loader;

public class CollectionLoader extends Loader<CollectionPlayer> {
    @Override
    public DBKeeper<CollectionPlayer> getKeeper() {
        return StreamlineCollections.getKeeper();
    }

    @Override
    public CollectionPlayer getConsole() {
        return null;
    }

    @Override
    public void fireLoadEvents(CollectionPlayer player) {
    }

    @Override
    public CollectionPlayer instantiate(String identifier) {
        return new CollectionPlayer(identifier);
    }

    @Override
    public void fireCreateEvents(CollectionPlayer player) {
    }
}
