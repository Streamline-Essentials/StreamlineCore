package net.streamline.api.savables.datalizable.database.connection;

public interface IConnector<C> {
    C buildConnection();
    void checkAndRebuild();
}
