package net.streamline.api.savables.datalizable.database.connection;

import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;

@Getter @Setter
public abstract class SQLConnector implements IConnector<Connection> {
    private Connection rawConnection;
    private ConnectorSet connectorSet;

    public SQLConnector(ConnectorSet connectorSet) {
        this.connectorSet = connectorSet;

        this.rawConnection = buildConnection();
    }

    @Override
    public abstract Connection buildConnection();

    @Override
    public void checkAndRebuild() {
        if (rawConnection == null) rawConnection = buildConnection();
        try {
            if (rawConnection.isClosed()) rawConnection = buildConnection();
        } catch (Exception e) {
            rawConnection = buildConnection();
        }
    }
}
