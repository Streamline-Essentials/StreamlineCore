package net.streamline.api.database.modules;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.database.CoreDBOperator;
import net.streamline.api.database.DatabaseType;
import tv.quaint.objects.Identifiable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public abstract class DBKeeper<T extends Identifiable> implements Identifiable {
    private String identifier;
    private ResourceGetter<T> getter;

    public DBKeeper(String identifier, ResourceGetter<T> getter) {
        this.identifier = identifier;
        this.getter = getter;
    }

    public static CoreDBOperator getDatabase() {
        return SLAPI.getMainDatabase();
    }

    public static DatabaseType getDatabaseType() {
        return getDatabase().getConnectorSet().getType();
    }

    public static String getTablePrefix() {
        return getDatabase().getConnectorSet().getTablePrefix();
    }

    public void ensureTables() {
        if (getDatabaseType() == DatabaseType.MYSQL) {
            ensureMysqlTables();
        } else if (getDatabaseType() == DatabaseType.SQLITE) {
            ensureSqliteTables();
        }
    }

    public abstract void ensureMysqlTables();

    public abstract void ensureSqliteTables();

    public void save(T obj, boolean async) {
        if (async) {
            CompletableFuture.runAsync(() -> saveRaw(obj));
        } else {
            saveRaw(obj);
        }
    }

    public void save(T obj) {
        save(obj, true);
    }

    public void saveRaw(T obj) {
        if (getDatabaseType() == DatabaseType.MYSQL) {
            saveMysql(obj);
        } else if (getDatabaseType() == DatabaseType.SQLITE) {
            saveSqlite(obj);
        }
    }

    public abstract void saveMysql(T obj);

    public abstract void saveSqlite(T obj);

    public CompletableFuture<Optional<T>> load(String identifier) {
        return CompletableFuture.supplyAsync(() -> loadRaw(identifier));
    }

    public Optional<T> loadRaw(String identifier) {
        if (getDatabaseType() == DatabaseType.MYSQL) {
            return loadMysql(identifier);
        } else if (getDatabaseType() == DatabaseType.SQLITE) {
            return loadSqlite(identifier);
        }
        return Optional.of(getGetter().apply(identifier));
    }

    public abstract Optional<T> loadMysql(String identifier);

    public abstract Optional<T> loadSqlite(String identifier);

    public boolean forceExists(String identifier) {
        return exists(identifier).join();
    }

    public CompletableFuture<Boolean> exists(String identifier) {
        return CompletableFuture.supplyAsync(() -> existsRaw(identifier));
    }

    public boolean existsRaw(String identifier) {
        if (getDatabaseType() == DatabaseType.MYSQL) {
            return existsMysql(identifier);
        } else if (getDatabaseType() == DatabaseType.SQLITE) {
            return existsSqlite(identifier);
        }
        return false;
    }

    public abstract boolean existsMysql(String identifier);

    public abstract boolean existsSqlite(String identifier);
}