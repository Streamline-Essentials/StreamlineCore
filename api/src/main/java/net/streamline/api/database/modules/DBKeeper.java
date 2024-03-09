package net.streamline.api.database.modules;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.database.CoreDBOperator;
import net.streamline.api.database.DatabaseType;
import org.jetbrains.annotations.NotNull;
import tv.quaint.savables.SavableResource;

import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public abstract class DBKeeper<T extends Comparable<T>> implements Comparable<DBKeeper<T>> {
    private T resource;
    private ResourceGetter<T> getter;

    public DBKeeper(T resource, ResourceGetter<T> getter) {
        this.resource = resource;
        this.getter = getter;
    }

    @Override
    public int compareTo(@NotNull DBKeeper<T> o) {
        return resource.compareTo(o.getResource());
    }

    public static CoreDBOperator getDatabase() {
        return SLAPI.getMainDatabase();
    }

    public static DatabaseType getDatabaseType() {
        return getDatabase().getConnectorSet().getType();
    }

    public void save(boolean async) {
        if (async) {
            CompletableFuture.runAsync(this::saveRaw);
        } else {
            saveRaw();
        }
    }

    public void save() {
        save(true);
    }

    public void saveRaw() {
        if (getDatabaseType() == DatabaseType.MYSQL) {
            saveMysql();
        } else if (getDatabaseType() == DatabaseType.SQLITE) {
            saveSqlite();
        }
    }

    public abstract void saveMysql();

    public abstract void saveSqlite();

    public CompletableFuture<T> load() {
        return CompletableFuture.supplyAsync(this::loadRaw);
    }

    public T loadRaw() {
        if (getDatabaseType() == DatabaseType.MYSQL) {
            return loadMysql();
        } else if (getDatabaseType() == DatabaseType.SQLITE) {
            return loadSqlite();
        }
        return getGetter().get();
    }

    public abstract T loadMysql();

    public abstract T loadSqlite();

    public boolean forceExists() {
        return exists().join();
    }

    public CompletableFuture<Boolean> exists() {
        return CompletableFuture.supplyAsync(this::existsRaw);
    }

    public boolean existsRaw() {
        if (getDatabaseType() == DatabaseType.MYSQL) {
            return existsMysql();
        } else if (getDatabaseType() == DatabaseType.SQLITE) {
            return existsSqlite();
        }
        return false;
    }

    public abstract boolean existsMysql();

    public abstract boolean existsSqlite();
}