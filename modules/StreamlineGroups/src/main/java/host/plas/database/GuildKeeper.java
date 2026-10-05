package host.plas.database;

import host.plas.data.Guild;
import host.plas.data.roles.SavableGroupRole;
import net.streamline.api.SLAPI;
import singularity.data.console.CosmicSender;
import singularity.database.modules.DBKeeper;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Persists {@link Guild} groups.
 *
 * <p>Guilds keep the state declared on {@code AbstractGroup} -- owner, visibility, mute
 * state, size cap and creation time -- in their own {@code guilds} table, separate from
 * the {@code grouped_players} table that {@link PlayerKeeper} owns.</p>
 */
public class GuildKeeper extends DBKeeper<Guild> {
    public GuildKeeper() {
        super("guilds", Guild::new);
    }

    @Override
    public void ensureMysqlTables() {
        String statement = "CREATE TABLE IF NOT EXISTS `%table_prefix%guilds` (" +
                "`Uuid` VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "`OwnerUuid` VARCHAR(36) NOT NULL, " +
                "`IsMuted` BIT NOT NULL, " +
                "`IsPublic` BIT NOT NULL, " +
                "`MaxSize` INT NOT NULL, " +
                "`CreateDate` BIGINT NOT NULL " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%guild_members` (" +
                "`GuildUuid` VARCHAR(36) NOT NULL, " +
                "`MemberUuid` VARCHAR(36) NOT NULL, " +
                "`RoleIdentifier` VARCHAR(128) NOT NULL, " +
                "PRIMARY KEY (`GuildUuid`, `MemberUuid`) " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, smt -> {});
    }

    @Override
    public void ensureSqliteTables() {
        String statement = "CREATE TABLE IF NOT EXISTS `%table_prefix%guilds` (" +
                "`Uuid` TEXT NOT NULL PRIMARY KEY, " +
                "`OwnerUuid` TEXT NOT NULL, " +
                "`IsMuted` INTEGER NOT NULL, " +
                "`IsPublic` INTEGER NOT NULL, " +
                "`MaxSize` INTEGER NOT NULL, " +
                "`CreateDate` INTEGER NOT NULL " +
                ");;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%guild_members` (" +
                "`GuildUuid` TEXT NOT NULL, " +
                "`MemberUuid` TEXT NOT NULL, " +
                "`RoleIdentifier` TEXT NOT NULL, " +
                "PRIMARY KEY (`GuildUuid`, `MemberUuid`) " +
                ");;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, smt -> {});
    }

    @Override
    public void saveMysql(Guild obj) {
        String statement = "INSERT INTO `%table_prefix%guilds` (" +
                "`Uuid`, `OwnerUuid`, `IsMuted`, `IsPublic`, `MaxSize`, `CreateDate`" +
                ") VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "`OwnerUuid` = ?, `IsMuted` = ?, `IsPublic` = ?, `MaxSize` = ?, `CreateDate` = ?;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, stmt -> {
            try {
                stmt.setString(1, obj.getUuid());
                stmt.setString(2, ownerUuidOf(obj));
                stmt.setBoolean(3, obj.isMuted());
                stmt.setBoolean(4, obj.isPublic());
                stmt.setInt(5, obj.getMaxSize());
                stmt.setLong(6, createMillisOf(obj));

                // Repeat everything but the primary key for the update half.
                stmt.setString(7, ownerUuidOf(obj));
                stmt.setBoolean(8, obj.isMuted());
                stmt.setBoolean(9, obj.isPublic());
                stmt.setInt(10, obj.getMaxSize());
                stmt.setLong(11, createMillisOf(obj));
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to save guild " + obj.getUuid(), e);
            }
        });

        saveMembers(obj);
    }

    @Override
    public void saveSqlite(Guild obj) {
        String statement = "INSERT OR REPLACE INTO `%table_prefix%guilds` (" +
                "`Uuid`, `OwnerUuid`, `IsMuted`, `IsPublic`, `MaxSize`, `CreateDate`" +
                ") VALUES (?, ?, ?, ?, ?, ?);";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, stmt -> {
            try {
                stmt.setString(1, obj.getUuid());
                stmt.setString(2, ownerUuidOf(obj));
                stmt.setBoolean(3, obj.isMuted());
                stmt.setBoolean(4, obj.isPublic());
                stmt.setInt(5, obj.getMaxSize());
                stmt.setLong(6, createMillisOf(obj));
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to save guild " + obj.getUuid(), e);
            }
        });

        saveMembers(obj);
    }

    /**
     * Rewrites the guild's membership rows, one per member, recording the role each one
     * currently holds. The set is replaced wholesale so that members who have left do not
     * linger.
     */
    private void saveMembers(Guild obj) {
        // One transaction on one connection, so that a concurrent load never reads the
        // membership between the delete and the inserts.
        try (Connection connection = getDatabase().getConnection()) {
            if (connection == null) {
                MessageUtils.logWarning("Could not obtain a connection to save the members of guild " + obj.getUuid());
                return;
            }

            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = connection.prepareStatement(
                        injectTablePrefix("DELETE FROM `%table_prefix%guild_members` WHERE `GuildUuid` = ?;"))) {
                    stmt.setString(1, obj.getUuid());
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix("INSERT INTO `%table_prefix%guild_members` " +
                        "(`GuildUuid`, `MemberUuid`, `RoleIdentifier`) VALUES (?, ?, ?);"))) {
                    // A member sits in one role; the set keeps a stray duplicate from
                    // failing the primary key and rolling the whole write back.
                    Set<String> written = new HashSet<>();
                    for (SavableGroupRole role : obj.getGroupRoleMap().getRoles()) {
                        for (CosmicSender member : obj.getGroupRoleMap().getUsersOf(role)) {
                            if (! written.add(member.getUuid())) continue;

                            stmt.setString(1, obj.getUuid());
                            stmt.setString(2, member.getUuid());
                            stmt.setString(3, role.getIdentifier());
                            stmt.addBatch();
                        }
                    }
                    stmt.executeBatch();
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                MessageUtils.logWarning("Failed to save the members of guild " + obj.getUuid(), e);
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (Exception e) {
            MessageUtils.logWarning("Failed to save the members of guild " + obj.getUuid(), e);
        }
    }

    /**
     * Restores the guild's members into the roles they held, skipping any role that is no
     * longer configured.
     *
     * <p>The rows are read into memory before any member is resolved: resolving a member
     * may query the database, and doing so inside the result callback would need a second
     * connection while the first is still held -- which, with SQLite's single-connection
     * pool, waits out the pool timeout and fails.</p>
     */
    private void loadMembers(Guild guild) {
        String statement = injectTablePrefix(
                "SELECT `MemberUuid`, `RoleIdentifier` FROM `%table_prefix%guild_members` WHERE `GuildUuid` = ?;");

        Map<String, String> rolesByMember = new LinkedHashMap<>();
        getDatabase().executeQuery(statement, stmt -> {
            try {
                stmt.setString(1, guild.getUuid());
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to bind uuid while loading guild members", e);
            }
        }, result -> {
            try {
                while (result.next()) {
                    rolesByMember.put(result.getString("MemberUuid"), result.getString("RoleIdentifier"));
                }
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to load members of guild " + guild.getUuid(), e);
            }
        });

        rolesByMember.forEach((memberUuid, roleIdentifier) -> {
            Optional<CosmicSender> member = UserUtils.getOrGetSender(memberUuid);
            if (member.isEmpty()) return;

            guild.getGroupRoleMap().getRoles().stream()
                    .filter(role -> role.getIdentifier().equals(roleIdentifier))
                    .findFirst()
                    .ifPresent(role -> guild.getGroupRoleMap().applyUser(role, member.get()));
        });
    }

    @Override
    public Optional<Guild> loadMysql(String identifier) {
        return loadBoth(identifier);
    }

    @Override
    public Optional<Guild> loadSqlite(String identifier) {
        return loadBoth(identifier);
    }

    public Optional<Guild> loadBoth(String identifier) {
        String statement = "SELECT * FROM `%table_prefix%guilds` WHERE `Uuid` = ?;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        // The callback only copies the row out; the owner lookup and the member query run
        // after it returns, for the reason given on loadMembers.
        AtomicReference<GuildRow> row = new AtomicReference<>();
        getDatabase().executeQuery(statement, stmt -> {
            try {
                stmt.setString(1, identifier);
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to bind uuid while loading a guild", e);
            }
        }, result -> {
            try {
                if (! result.next()) return;

                row.set(new GuildRow(
                        result.getString("Uuid"),
                        result.getString("OwnerUuid"),
                        result.getBoolean("IsMuted"),
                        result.getBoolean("IsPublic"),
                        result.getInt("MaxSize"),
                        result.getLong("CreateDate")));
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to load guild " + identifier, e);
            }
        });

        GuildRow r = row.get();
        if (r == null) return Optional.empty();

        try {
            // Constructed with loading disabled, and without a database fetch of its own,
            // so that building the guild does not re-enter this keeper while the load is
            // still in flight.
            Optional<CosmicSender> owner = UserUtils.getOrGetSender(r.ownerUuid);
            Guild g = Guild.hydrated(r.uuid, owner.orElse(null));

            g.setMuted(r.muted);
            g.setPublic(r.isPublic);
            g.setMaxSize(r.maxSize);
            g.setCreateDate(new Date(r.createDate));

            loadMembers(g);

            return Optional.of(g);
        } catch (Exception e) {
            MessageUtils.logWarning("Failed to load guild " + identifier, e);
            return Optional.empty();
        }
    }

    /** The columns of one {@code guilds} row. */
    private static final class GuildRow {
        private final String uuid;
        private final String ownerUuid;
        private final boolean muted;
        private final boolean isPublic;
        private final int maxSize;
        private final long createDate;

        private GuildRow(String uuid, String ownerUuid, boolean muted, boolean isPublic, int maxSize, long createDate) {
            this.uuid = uuid;
            this.ownerUuid = ownerUuid;
            this.muted = muted;
            this.isPublic = isPublic;
            this.maxSize = maxSize;
            this.createDate = createDate;
        }
    }

    @Override
    public boolean existsMysql(String identifier) {
        return existsBoth(identifier);
    }

    @Override
    public boolean existsSqlite(String identifier) {
        return existsBoth(identifier);
    }

    public boolean existsBoth(String identifier) {
        String statement = injectTablePrefix("SELECT `Uuid` FROM `%table_prefix%guilds` WHERE `Uuid` = ?;");

        AtomicReference<Boolean> exists = new AtomicReference<>(false);
        getDatabase().executeQuery(statement, stmt -> {
            try {
                stmt.setString(1, identifier);
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to bind uuid while checking for a guild", e);
            }
        }, result -> {
            try {
                exists.set(result.next());
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to check for guild " + identifier, e);
            }
        });

        return exists.get();
    }

    @Override
    public boolean deleteMysql(String identifier) {
        return deleteBoth(identifier);
    }

    @Override
    public boolean deleteSqlite(String identifier) {
        return deleteBoth(identifier);
    }

    public boolean deleteBoth(String identifier) {
        String statement = "DELETE FROM `%table_prefix%guilds` WHERE `Uuid` = ?;;" +
                "DELETE FROM `%table_prefix%guild_members` WHERE `GuildUuid` = ?;;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        return deleteWith(identifier, statement);
    }

    /**
     * Loads every stored guild.
     *
     * @return a future resolving to all guilds currently persisted
     */
    public CompletableFuture<ConcurrentSkipListSet<Guild>> pullAllGuilds() {
        return CompletableFuture.supplyAsync(() -> {
            String statement = "SELECT `Uuid` FROM `%table_prefix%guilds`;";

            statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

            ConcurrentSkipListSet<String> uuids = new ConcurrentSkipListSet<>();
            getDatabase().executeQuery(statement, stmt -> {}, result -> {
                try {
                    while (result.next()) {
                        uuids.add(result.getString("Uuid"));
                    }
                } catch (Exception e) {
                    MessageUtils.logWarning("Failed to list guilds", e);
                }
            });

            ConcurrentSkipListSet<Guild> guilds = new ConcurrentSkipListSet<>();
            uuids.forEach(uuid -> loadBoth(uuid).ifPresent(guilds::add));

            return guilds;
        });
    }

    /**
     * A guild may be constructed before an owner is resolved, so the owner uuid is
     * stored as an empty string rather than failing the write.
     */
    private String ownerUuidOf(Guild guild) {
        return guild.getOwner() == null ? "" : guild.getOwner().getUuid();
    }

    private long createMillisOf(Guild guild) {
        return guild.getCreateDate() == null ? System.currentTimeMillis() : guild.getCreateDate().getTime();
    }
}
