package host.plas.database;

import host.plas.data.Guild;
import net.streamline.api.SLAPI;
import singularity.data.console.CosmicSender;
import singularity.database.modules.DBKeeper;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.Date;
import java.util.Optional;
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
        String delete = injectTablePrefix("DELETE FROM `%table_prefix%guild_members` WHERE `GuildUuid` = ?;;");
        deleteWith(obj.getUuid(), delete);

        String insert = injectTablePrefix("INSERT INTO `%table_prefix%guild_members` " +
                "(`GuildUuid`, `MemberUuid`, `RoleIdentifier`) VALUES (?, ?, ?);");

        obj.getGroupRoleMap().getRoles().forEach(role ->
                obj.getGroupRoleMap().getUsersOf(role).forEach(member ->
                        getDatabase().execute(insert, stmt -> {
                            try {
                                stmt.setString(1, obj.getUuid());
                                stmt.setString(2, member.getUuid());
                                stmt.setString(3, role.getIdentifier());
                            } catch (Exception e) {
                                MessageUtils.logWarning("Failed to save a member of guild " + obj.getUuid(), e);
                            }
                        })));
    }

    /**
     * Restores the guild's members into the roles they held, skipping any role that is no
     * longer configured.
     */
    private void loadMembers(Guild guild) {
        String statement = injectTablePrefix(
                "SELECT `MemberUuid`, `RoleIdentifier` FROM `%table_prefix%guild_members` WHERE `GuildUuid` = ?;");

        getDatabase().executeQuery(statement, stmt -> {
            try {
                stmt.setString(1, guild.getUuid());
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to bind uuid while loading guild members", e);
            }
        }, result -> {
            try {
                while (result.next()) {
                    String memberUuid = result.getString("MemberUuid");
                    String roleIdentifier = result.getString("RoleIdentifier");

                    Optional<CosmicSender> member = UserUtils.getOrGetSender(memberUuid);
                    if (member.isEmpty()) continue;

                    guild.getGroupRoleMap().getRoles().stream()
                            .filter(role -> role.getIdentifier().equals(roleIdentifier))
                            .findFirst()
                            .ifPresent(role -> guild.getGroupRoleMap().applyUser(role, member.get()));
                }
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to load members of guild " + guild.getUuid(), e);
            }
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

        AtomicReference<Optional<Guild>> guild = new AtomicReference<>(Optional.empty());
        getDatabase().executeQuery(statement, stmt -> {
            try {
                stmt.setString(1, identifier);
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to bind uuid while loading a guild", e);
            }
        }, result -> {
            try {
                if (! result.next()) return;

                String uuid = result.getString("Uuid");
                String ownerUuid = result.getString("OwnerUuid");

                // Constructed with loading disabled, and without a database fetch of its
                // own, so that building the guild does not re-enter this keeper while the
                // load is still in flight.
                Optional<CosmicSender> owner = UserUtils.getOrGetSender(ownerUuid);
                Guild g = Guild.hydrated(uuid, owner.orElse(null));

                g.setMuted(result.getBoolean("IsMuted"));
                g.setPublic(result.getBoolean("IsPublic"));
                g.setMaxSize(result.getInt("MaxSize"));
                g.setCreateDate(new Date(result.getLong("CreateDate")));

                loadMembers(g);

                guild.set(Optional.of(g));
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to load guild " + identifier, e);
            }
        });

        return guild.get();
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
        return loadBoth(identifier).isPresent();
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
