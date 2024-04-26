package net.streamline.api.database;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.uuid.UuidInfo;

public class CoreDBOperator extends DBOperator {
    @Getter @Setter
    private static ConcurrentSkipListMap<String, CompletableFuture<Optional<StreamPlayer>>> loadingPlayers = new ConcurrentSkipListMap<>();

    public CoreDBOperator(ConnectorSet set) {
        super(set, "StreamlineCore");
    }

    @Override
    public void ensureDatabase() {
        String s1 = Statements.getStatement(Statements.StatementType.CREATE_DATABASE, this.getConnectorSet());
        if (s1 == null) return;
        if (s1.isBlank() || s1.isEmpty()) return;

        this.execute(s1, stmt -> {});
    }

    @Override
    public void ensureTables() {
        String s1 = Statements.getStatement(Statements.StatementType.CREATE_TABLES, this.getConnectorSet());
        if (s1 == null) return;
        if (s1.isBlank() || s1.isEmpty()) return;

        this.execute(s1, stmt -> {});
    }

    public void savePlayer(StreamPlayer player, boolean async) {
        if (async) {
            CompletableFuture.runAsync(() -> savePlayerAsync(player).join());
        } else {
            savePlayerAsync(player).join();
        }
    }

    public void savePlayer(StreamPlayer player) {
        savePlayer(player, true);
    }

    public boolean isPlayerTouched(String uuid) {
        ensureUsable();

        String s1 = Statements.getStatement(Statements.StatementType.PLAYER_IS_TOUCHED, this.getConnectorSet());
        if (s1 == null) return true;
        if (s1.isBlank() || s1.isEmpty()) return true;

        AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        
        this.executeQuery(s1, stmt -> {
            try {
                stmt.setString(1, uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, rs -> {
            try {
                if (rs.next()) {
                    boolean isTouched = rs.getBoolean("ProxyTouched");
                    
                    atomicBoolean.set(isTouched);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        return atomicBoolean.get();
    }
    
    private CompletableFuture<Boolean> savePlayerAsync(StreamPlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            String s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_MAIN, this.getConnectorSet());
            if (s1 == null) return false;
            if (s1.isBlank() || s1.isEmpty()) return false;

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, player.getUuid());
                    stmt.setLong(2, player.getFirstJoinDate().getTime());
                    stmt.setLong(3, player.getLastJoinDate().getTime());
                    stmt.setString(4, player.getCurrentName());
                    stmt.setString(5, player.getCurrentIp());
                    stmt.setLong(6, player.getPlaySeconds());
                    stmt.setDouble(7, player.getPoints());
                    stmt.setBoolean(8, player.isProxyTouched());

                    // Repeat everything except the Uuid parameter for MySql.
                    if (getType() == DatabaseType.MYSQL) {
                        stmt.setLong(9, player.getFirstJoinDate().getTime());
                        stmt.setLong(10, player.getLastJoinDate().getTime());
                        stmt.setString(11, player.getCurrentName());
                        stmt.setString(12, player.getCurrentIp());
                        stmt.setLong(13, player.getPlaySeconds());
                        stmt.setDouble(14, player.getPoints());
                        stmt.setBoolean(15, player.isProxyTouched());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            if (player.getMeta() != null) {
                s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_META, this.getConnectorSet());
                if (s1 == null) return false;
                if (s1.isBlank() || s1.isEmpty()) return false;

                this.execute(s1, stmt -> {
                    try {
                        stmt.setString(1, player.getUuid());
                        stmt.setString(2, player.getMeta().getNickname());
                        stmt.setString(3, player.getMeta().getPrefix());
                        stmt.setString(4, player.getMeta().getSuffix());

                        // Repeat everything except the Uuid parameter (which is the first parameter) for MySql.
                        if (getType() == DatabaseType.MYSQL) {
                            stmt.setString(5, player.getMeta().getNickname());
                            stmt.setString(6, player.getMeta().getPrefix());
                            stmt.setString(7, player.getMeta().getSuffix());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            if (player.getLeveling() != null) {
                s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_LEVELING, this.getConnectorSet());
                if (s1 == null) return false;
                if (s1.isBlank() || s1.isEmpty()) return false;

                this.execute(s1, stmt -> {
                    try {
                        stmt.setString(1, player.getUuid());
                        stmt.setInt(2, player.getLeveling().getLevel());
                        stmt.setDouble(3, player.getLeveling().getTotalExperience());
                        stmt.setDouble(4, player.getLeveling().getCurrentExperience());
                        stmt.setString(5, player.getLeveling().getEquationString());
                        stmt.setInt(6, player.getLeveling().getStartedLevel());
                        stmt.setDouble(7, player.getLeveling().getStartedExperience());

                        // Repeat everything except the Uuid parameter for MySql.
                        if (getType() == DatabaseType.MYSQL) {
                            stmt.setInt(8, player.getLeveling().getLevel());
                            stmt.setDouble(9, player.getLeveling().getTotalExperience());
                            stmt.setDouble(10, player.getLeveling().getCurrentExperience());
                            stmt.setString(11, player.getLeveling().getEquationString());
                            stmt.setInt(12, player.getLeveling().getStartedLevel());
                            stmt.setDouble(13, player.getLeveling().getStartedExperience());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            if (player.getLocation() != null) {
                s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_LOCATION, this.getConnectorSet());
                if (s1 == null) return false;
                if (s1.isBlank() || s1.isEmpty()) return false;

                this.execute(s1, stmt -> {
                    try {
                        stmt.setString(1, player.getUuid());
                        stmt.setString(2, player.getLocation().getServerName());
                        stmt.setString(3, player.getLocation().getWorldName());
                        stmt.setDouble(4, player.getLocation().getX());
                        stmt.setDouble(5, player.getLocation().getY());
                        stmt.setDouble(6, player.getLocation().getZ());
                        stmt.setFloat(7, player.getLocation().getYaw());
                        stmt.setFloat(8, player.getLocation().getPitch());

                        // Repeat everything except the Uuid parameter for MySql.
                        if (getType() == DatabaseType.MYSQL) {
                            stmt.setString(9, player.getLocation().getServerName());
                            stmt.setString(10, player.getLocation().getWorldName());
                            stmt.setDouble(11, player.getLocation().getX());
                            stmt.setDouble(12, player.getLocation().getY());
                            stmt.setDouble(13, player.getLocation().getZ());
                            stmt.setFloat(14, player.getLocation().getYaw());
                            stmt.setFloat(15, player.getLocation().getPitch());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            if (player.getPermissions() != null) {
                s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_PERMISSIONS, this.getConnectorSet());
                if (s1 == null) return false;
                if (s1.isBlank() || s1.isEmpty()) return false;

                s1 = s1.replace("%uuid%", player.getUuid());
                s1 = s1.replace("%bypassing_permissions%", String.valueOf(player.getPermissions().isBypassingPermissions()));

                this.execute(s1, stmt -> {
                    try {
                        stmt.setString(1, player.getUuid());
                        stmt.setBoolean(2, player.getPermissions().isBypassingPermissions());

                        // Repeat everything except the Uuid parameter for MySql.
                        if (getType() == DatabaseType.MYSQL) {
                            stmt.setBoolean(3, player.getPermissions().isBypassingPermissions());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            return true;
        });
    }

    public CompletableFuture<Optional<StreamPlayer>> loadPlayer(String uuid) {
        CompletableFuture<Optional<StreamPlayer>> future = getLoadingPlayers().get(uuid);
        if (future == null || future.isDone()) {
            CompletableFuture<Optional<StreamPlayer>> loading = CompletableFuture.supplyAsync(() -> {
                ensureUsable();

                if (! exists(uuid).join()) return Optional.empty();

                StreamPlayer player = new StreamPlayer(uuid);

                String s1 = Statements.getStatement(Statements.StatementType.PULL_PLAYER_MAIN, this.getConnectorSet());
                if (s1 == null) return Optional.empty();
                if (s1.isBlank() || s1.isEmpty()) return Optional.empty();

                this.executeQuery(s1, stmt -> {
                    try {
                        stmt.setString(1, uuid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, rs -> {
                    try {
                        if (rs.next()) {
                            player.setFirstJoinMillis(rs.getLong("FirstJoin"));
                            player.setLastJoinMillis(rs.getLong("LastJoin"));
                            player.setCurrentName(rs.getString("CurrentName"));
                            player.setCurrentIp(rs.getString("CurrentIP"));
                            player.setPlaySeconds(rs.getLong("PlaySeconds"));
                            player.setPoints(rs.getInt("Points"));
                            player.setProxyTouched(rs.getBoolean("ProxyTouched"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                String s2 = Statements.getStatement(Statements.StatementType.PULL_PLAYER_META, this.getConnectorSet());
                if (s2 == null) return Optional.empty();
                if (s2.isBlank() || s2.isEmpty()) return Optional.empty();

                this.executeQuery(s2, stmt -> {
                    try {
                        stmt.setString(1, uuid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, rs -> {
                    try {
                        if (rs.next()) {
                            player.getMeta().setNickname(rs.getString("Nickname"));
                            player.getMeta().setPrefix(rs.getString("Prefix"));
                            player.getMeta().setSuffix(rs.getString("Suffix"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                String s3 = Statements.getStatement(Statements.StatementType.PULL_PLAYER_LEVELING, this.getConnectorSet());
                if (s3 == null) return Optional.empty();
                if (s3.isBlank() || s3.isEmpty()) return Optional.empty();

                this.executeQuery(s3, stmt -> {
                    try {
                        stmt.setString(1, uuid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, rs -> {
                    try {
                        if (rs.next()) {
                            player.getLeveling().setLevel(rs.getInt("Level"));
                            player.getLeveling().setTotalExperience(rs.getDouble("TotalExperience"));
                            player.getLeveling().setCurrentExperience(rs.getDouble("CurrentExperience"));
                            player.getLeveling().setEquationString(rs.getString("EquationString"));
                            player.getLeveling().setStartedLevel(rs.getInt("StartedLevel"));
                            player.getLeveling().setStartedExperience(rs.getDouble("StartedExperience"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                String s4 = Statements.getStatement(Statements.StatementType.PULL_PLAYER_LOCATION, this.getConnectorSet());
                if (s4 == null) return Optional.empty();
                if (s4.isBlank() || s4.isEmpty()) return Optional.empty();

                this.executeQuery(s4, stmt -> {
                    try {
                        stmt.setString(1, uuid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, rs -> {
                    try {
                        if (rs.next()) {
                            player.getLocation().setServerName(rs.getString("Server"));
                            player.getLocation().setWorldName(rs.getString("World"));
                            player.getLocation().setX(rs.getDouble("X"));
                            player.getLocation().setY(rs.getDouble("Y"));
                            player.getLocation().setZ(rs.getDouble("Z"));
                            player.getLocation().setYaw(rs.getFloat("Yaw"));
                            player.getLocation().setPitch(rs.getFloat("Pitch"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                String s5 = Statements.getStatement(Statements.StatementType.PULL_PLAYER_PERMISSIONS, this.getConnectorSet());
                if (s5 == null) return Optional.empty();
                if (s5.isBlank() || s5.isEmpty()) return Optional.empty();

                this.executeQuery(s5, stmt -> {
                    try {
                        stmt.setString(1, uuid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, rs -> {
                    try {
                        if (rs.next()) {
                            player.getPermissions().setBypassingPermissions(rs.getBoolean("BypassingPermissions"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                getLoadingPlayers().remove(uuid);

                return Optional.of(player);
            });

            getLoadingPlayers().put(uuid, loading);

            future = loading;
        }

        return future;
    }

    public CompletableFuture<Boolean> exists(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PLAYER_EXISTS, this.getConnectorSet());
            if (s1 == null) return false;
            if (s1.isBlank() || s1.isEmpty()) return false;

            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            this.executeQuery(s1, stmt -> {
                try {
                    stmt.setString(1, uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, rs -> {
                try {
                    atomicBoolean.set(rs.next());
                } catch (Exception e) {
                    e.printStackTrace();
                    atomicBoolean.set(false);
                }
            });

            return atomicBoolean.get();
        });
    }

    public CompletableFuture<Boolean> saveUuidInfo(UuidInfo uuidInfo) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PUSH_UUID_INFO, this.getConnectorSet());
            if (s1 == null) return false;
            if (s1.isBlank() || s1.isEmpty()) return false;

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, uuidInfo.getUuid());
                    stmt.setString(2, uuidInfo.computableNames());
                    stmt.setString(3, uuidInfo.computableIps());

                    if (getType() == DatabaseType.MYSQL) {
                        stmt.setString(4, uuidInfo.computableNames());
                        stmt.setString(5, uuidInfo.computableIps());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return true;
        });
    }

    public CompletableFuture<Optional<UuidInfo>> loadUuidInfo(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PULL_UUID_INFO, this.getConnectorSet());
            if (s1 == null) return Optional.empty();
            if (s1.isBlank() || s1.isEmpty()) return Optional.empty();

            s1 = s1.replace("%uuid%", uuid);

            AtomicReference<Optional<UuidInfo>> uuidInfo = new AtomicReference<>(Optional.empty());
            this.executeQuery(s1, stmt -> {
                try {
                    stmt.setString(1, uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, rs -> {
                try {
                    if (rs.next()) {
                        String names = rs.getString("Usernames");
                        String ips = rs.getString("Ips");

                        UuidInfo info = new UuidInfo(uuid, names, ips);

                        uuidInfo.set(Optional.of(info));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return uuidInfo.get();
        });
    }

    public CompletableFuture<ConcurrentSkipListSet<UuidInfo>> pullAllUuidInfo() {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PULL_ALL_UUID_INFO, this.getConnectorSet());
            if (s1 == null) return new ConcurrentSkipListSet<>();
            if (s1.isBlank() || s1.isEmpty()) return new ConcurrentSkipListSet<>();

            AtomicReference<ConcurrentSkipListSet<UuidInfo>> uuids = new AtomicReference<>(new ConcurrentSkipListSet<>());
            this.executeQuery(s1, stmt -> {}, rs -> {
                try {
                    while (rs.next()) {
                        String uuid = rs.getString("Uuid");
                        String names = rs.getString("Usernames");
                        String ips = rs.getString("Ips");

                        UuidInfo info = new UuidInfo(uuid, names, ips);

                        uuids.get().add(info);
                    }
                } catch (SQLException e) {
                    if (e.getMessage().contains("duplicate")) return;
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return uuids.get();
        });
    }
}
