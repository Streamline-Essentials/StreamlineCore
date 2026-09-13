package host.plas.database;

import host.plas.discord.data.verified.VerifiedUser;
import singularity.database.modules.DBKeeper;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

public class VerifiedUserKeeper extends DBKeeper<VerifiedUser> {
    public VerifiedUserKeeper() {
        super("verifieduser-keeper", VerifiedUser::new);
    }

    @Override
    public void ensureMysqlTables() {
        String s1 = "CREATE TABLE IF NOT EXISTS %table_prefix%discord_verified_users (" +
                "Uuid VARCHAR(36) NOT NULL," +
                "DiscordIds TEXT NOT NULL," +
                "PreferredId TEXT NOT NULL," +
                "PRIMARY KEY (Uuid)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(s1, stmt -> {});
    }

    @Override
    public void ensureSqliteTables() {
        String s1 = "CREATE TABLE IF NOT EXISTS %table_prefix%discord_verified_users (" +
                "Uuid VARCHAR(36) NOT NULL," +
                "DiscordIds TEXT NOT NULL," +
                "PreferredId TEXT NOT NULL," +
                "PRIMARY KEY (Uuid)" +
                ");";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(s1, stmt -> {});
    }

    @Override
    public void saveMysql(VerifiedUser user) {
        ensureTables();

        String s1 = "INSERT INTO %table_prefix%discord_verified_users (" +
                "Uuid, DiscordIds, PreferredId" +
                ") VALUES (" +
                "?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE " +
                "DiscordIds = ?," +
                "PreferredId = ?" +
                ";";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", user.getIdentifier());
        s1 = s1.replace("%type%", user.getDiscordIdsAsString());
        s1 = s1.replace("%preferredId%", user.getPreferredDiscordIdOptional().map(String::valueOf).orElse(""));

        getDatabase().execute(s1, stmt -> {
            try {
                stmt.setString(1, user.getIdentifier());
                stmt.setString(2, user.getDiscordIdsAsString());
                stmt.setString(3, user.getPreferredDiscordIdOptional().map(String::valueOf).orElse(""));

                stmt.setString(4, user.getDiscordIdsAsString());
                stmt.setString(5, user.getPreferredDiscordIdOptional().map(String::valueOf).orElse(""));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void saveSqlite(VerifiedUser user) {
        ensureTables();

        String s1 = "INSERT OR REPLACE INTO %table_prefix%discord_verified_users (" +
                "Uuid, DiscordIds, PreferredId" +
                ") VALUES (" +
                "?, ?, ?" +
                ");";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", user.getIdentifier());
        s1 = s1.replace("%type%", user.getDiscordIdsAsString());
        s1 = s1.replace("%preferredId%", user.getPreferredDiscordIdOptional().map(String::valueOf).orElse(""));

        getDatabase().execute(s1, stmt -> {
            try {
                stmt.setString(1, user.getIdentifier());
                stmt.setString(2, user.getDiscordIdsAsString());
                stmt.setString(3, user.getPreferredDiscordIdOptional().map(String::valueOf).orElse(""));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Optional<VerifiedUser> loadMysql(String s) {
        ensureTables();

        String s1 = "SELECT * FROM %table_prefix%discord_verified_users WHERE Uuid = ?;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", s);

        AtomicReference<Optional<VerifiedUser>> atomicReference = new AtomicReference<>(Optional.empty());
        getDatabase().executeQuery(s1, stmt -> {
            try {
                stmt.setString(1, s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, resultSet -> {
            try {
                if (resultSet.next()) {
                    String uuid = resultSet.getString("Uuid");
                    String discordIds = resultSet.getString("DiscordIds");
                    String preferredId = resultSet.getString("PreferredId");

                    Long preferredDiscordId;
                    try {
                        preferredDiscordId = preferredId.isBlank() ? null : Long.parseLong(preferredId);
                    } catch (Throwable t) {
                        preferredDiscordId = null; // If parsing fails, set preferredId to null
                    }

                    VerifiedUser user = new VerifiedUser(uuid);
                    user.mapStringToDiscordId(discordIds);
                    if (preferredDiscordId != null) {
                        user.setPreferredDiscordId(preferredDiscordId);
                    }

                    atomicReference.set(Optional.of(user));
                } else {
                    atomicReference.set(Optional.empty());
                }
            } catch (Exception e) {
                e.printStackTrace();
                atomicReference.set(Optional.empty());
            }
        });

        return atomicReference.get();
    }

    @Override
    public Optional<VerifiedUser> loadSqlite(String s) {
        ensureTables();

        String s1 = "SELECT * FROM %table_prefix%discord_verified_users WHERE Uuid = ?;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", s);

        AtomicReference<Optional<VerifiedUser>> atomicReference = new AtomicReference<>(Optional.empty());
        getDatabase().executeQuery(s1, stmt -> {
            try {
                stmt.setString(1, s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, resultSet -> {
            try {
                if (resultSet.next()) {
                    String uuid = resultSet.getString("Uuid");
                    String discordIds = resultSet.getString("DiscordIds");
                    String preferredId = resultSet.getString("PreferredId");

                    Long preferredDiscordId;
                    try {
                        preferredDiscordId = preferredId.isBlank() ? null : Long.parseLong(preferredId);
                    } catch (Throwable t) {
                        preferredDiscordId = null; // If parsing fails, set preferredId to null
                    }

                    VerifiedUser user = new VerifiedUser(uuid);
                    user.mapStringToDiscordId(discordIds);
                    if (preferredDiscordId != null) {
                        user.setPreferredDiscordId(preferredDiscordId);
                    }

                    atomicReference.set(Optional.of(user));
                } else {
                    atomicReference.set(Optional.empty());
                }
            } catch (Exception e) {
                e.printStackTrace();
                atomicReference.set(Optional.empty());
            }
        });

        return atomicReference.get();
    }

    @Override
    public boolean existsMysql(String s) {
        ensureTables();

        String s1 = "SELECT * FROM %table_prefix%discord_verified_users WHERE Uuid = ?;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", s);

        AtomicReference<Boolean> atomicReference = new AtomicReference<>(false);
        getDatabase().executeQuery(s1, stmt -> {
            try {
                stmt.setString(1, s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, resultSet -> {
            try {
                boolean b = resultSet.next();

                atomicReference.set(b);
            } catch (Exception e) {
                e.printStackTrace();
                atomicReference.set(false);
            }
        });

        return atomicReference.get();
    }

    @Override
    public boolean existsSqlite(String s) {
        ensureTables();

        String s1 = "SELECT * FROM %table_prefix%discord_verified_users WHERE Uuid = ?;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", s);

        AtomicReference<Boolean> atomicReference = new AtomicReference<>(false);
        getDatabase().executeQuery(s1, stmt -> {
            try {
                stmt.setString(1, s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, resultSet -> {
            try {
                boolean b = resultSet.next();

                atomicReference.set(b);
            } catch (Exception e) {
                e.printStackTrace();
                atomicReference.set(false);
            }
        });

        return atomicReference.get();
    }

    public void drop(String id) {
        ensureTables();

        String s1 = "DELETE FROM %table_prefix%discord_verified_users WHERE Uuid = ?;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(s1, stmt -> {
            try {
                stmt.setString(1, id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void drop(VerifiedUser user) {
        drop(user.getIdentifier());
    }

    public ConcurrentSkipListSet<VerifiedUser> getAll() {
        ensureTables();

        String s1 = "SELECT * FROM %table_prefix%discord_verified_users;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        ConcurrentSkipListSet<VerifiedUser> users = new ConcurrentSkipListSet<>();
        getDatabase().executeQuery(s1, stmt -> {}, resultSet -> {
            try {
                while (resultSet.next()) {
                    String uuid = resultSet.getString("Uuid");
                    String discordIds = resultSet.getString("DiscordIds");
                    String preferredId = resultSet.getString("PreferredId");

                    VerifiedUser user = new VerifiedUser(uuid);
                    user.mapStringToDiscordId(discordIds);
                    try {
                        if (! preferredId.isBlank()) {
                            user.setPreferredDiscordId(Long.parseLong(preferredId));
                        }
                    } catch (Throwable t) {
                        // do nothing, preferredId will just be null
                    }

                    users.add(user);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return users;
    }
}
