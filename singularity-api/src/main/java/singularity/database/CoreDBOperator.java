package singularity.database;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.Setter;
import singularity.configs.given.GivenConfigs;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;
import singularity.data.teleportation.TPTicket;
import singularity.data.update.UpdateType;
import singularity.data.update.defaults.DefaultUpdaters;
import singularity.data.uuid.UuidInfo;
import singularity.database.servers.SavedServer;
import singularity.database.servers.UpdateInfo;
import singularity.interfaces.ISingularityExtension;
import singularity.utils.UserUtils;

public class CoreDBOperator extends DBOperator {
    @Getter @Setter
    private static AsyncCache<String, Optional<CosmicPlayer>> loadingPlayers = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .buildAsync();
            ;

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

    public void savePlayer(CosmicPlayer player, boolean async) {
        if (async) {
            CompletableFuture.runAsync(() -> savePlayerAsync(player).join());
        } else {
            savePlayerAsync(player).join();
        }
    }

    public void savePlayer(CosmicPlayer player) {
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
    
    private CompletableFuture<Boolean> savePlayerAsync(CosmicPlayer player) {
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
                    stmt.setBoolean(7, player.isProxyTouched());

                    // Repeat everything except the Uuid parameter for MySql.
                    if (getType() == DatabaseType.MYSQL) {
                        stmt.setLong(8, player.getFirstJoinDate().getTime());
                        stmt.setLong(9, player.getLastJoinDate().getTime());
                        stmt.setString(10, player.getCurrentName());
                        stmt.setString(11, player.getCurrentIp());
                        stmt.setLong(12, player.getPlaySeconds());
                        stmt.setBoolean(13, player.isProxyTouched());
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

            DefaultUpdaters.getPlayerUpdater().update(player.getUuid());

            return true;
        });
    }

    public CompletableFuture<Optional<CosmicPlayer>> loadPlayer(String uuid) {
        CompletableFuture<Optional<CosmicPlayer>> future = getLoadingPlayers().getIfPresent(uuid);
        if (future == null || future.isDone()) {
            CompletableFuture<Optional<CosmicPlayer>> loading = CompletableFuture.supplyAsync(() -> {
                ensureUsable();

                if (! exists(uuid).join()) {
                    CosmicPlayer player = UserUtils.createPlayer(uuid);
                    savePlayer(player, false);

                    return Optional.of(player);
                }

                CosmicPlayer player = new CosmicPlayer(uuid);

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

                getLoadingPlayers().synchronous().invalidate(uuid);

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

    public void clearUpdateAsync(UpdateType<?> updateType, String identifier) {
        CompletableFuture.runAsync(() -> clearUpdate(updateType, identifier).join());
    }

    public CompletableFuture<Void> clearUpdate(UpdateType<?> updateType, String identifier) {
        return CompletableFuture.runAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.CLEAR_UPDATE, this.getConnectorSet());

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, updateType.getIdentifier());
                    stmt.setString(2, identifier);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    public void postUpdateAsync(UpdateType<?> updateType, String identifier) {
        CompletableFuture.runAsync(() -> postUpdate(updateType, identifier).join());
    }

    public CompletableFuture<Void> postUpdate(UpdateType<?> updateType, String identifier) {
        return CompletableFuture.runAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PUT_UPDATE, this.getConnectorSet());

            SavedServer server = GivenConfigs.getServer();

            long time = System.currentTimeMillis();

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, updateType.getIdentifier());
                    stmt.setString(2, identifier);
                    stmt.setString(3, server.getIdentifier());
                    stmt.setLong(4, time);

                    if (getType() == DatabaseType.MYSQL) {
                        stmt.setString(5, server.getIdentifier());
                        stmt.setLong(6, time);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    public CompletableFuture<Optional<UpdateInfo>> checkUpdate(UpdateType<?> updateType, String identifier) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.CHECK_UPDATE, this.getConnectorSet());

            AtomicReference<Optional<UpdateInfo>> date = new AtomicReference<>(Optional.empty());

            this.executeQuery(s1, stmt -> {
                try {
                    stmt.setString(1, updateType.getIdentifier());
                    stmt.setString(2, identifier);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, rs -> {
                try {
                    if (rs.next()) {
                        try {
                            String serverUuid = rs.getString("ServerUuid");
                            long time = rs.getLong("PostDate");
                            Date d = new Date(time);

                            UpdateInfo info = new UpdateInfo(d, serverUuid);

                            date.set(Optional.of(info));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return date.get();
        });
    }

    public void putServerAsync(SavedServer server) {
        CompletableFuture.runAsync(() -> putServer(server).join());
    }

    public CompletableFuture<Void> putServer(SavedServer server) {
        return CompletableFuture.runAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PUT_SERVER, this.getConnectorSet());

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, server.getIdentifier());
                    stmt.setString(2, server.getName());
                    stmt.setString(3, server.getType().name());

                    if (getType() == DatabaseType.MYSQL) {
                        stmt.setString(4, server.getName());
                        stmt.setString(5, server.getType().name());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    public CompletableFuture<Optional<SavedServer>> pullServer(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PULL_SERVER, this.getConnectorSet());
            if (s1.isBlank()) return Optional.empty();

            AtomicReference<Optional<SavedServer>> server = new AtomicReference<>(Optional.empty());
            this.executeQuery(s1, stmt -> {
                try {
                    stmt.setString(1, uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, rs -> {
                try {
                    if (rs.next()) {
                        try {
                            String name = rs.getString("Name");
                            String type = rs.getString("Type");

                            ISingularityExtension.ServerType t = ISingularityExtension.ServerType.valueOf(type);

                            SavedServer s = new SavedServer(uuid, name, t);

                            server.set(Optional.of(s));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return server.get();
        });
    }

    public void postTPTicketAsync(TPTicket ticket) {
        CompletableFuture.runAsync(() -> postTPTicket(ticket).join());
    }

    public CompletableFuture<Void> postTPTicket(TPTicket ticket) {
        return CompletableFuture.runAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PUT_TP_TICKET, this.getConnectorSet());
            if (s1.isBlank()) return;

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, ticket.getIdentifier());
                    stmt.setString(2, ticket.getTargetServer().getIdentifier());
                    stmt.setString(3, ticket.getTargetWorld().getIdentifier());
                    stmt.setDouble(4, ticket.getTargetLocation().getX());
                    stmt.setDouble(5, ticket.getTargetLocation().getY());
                    stmt.setDouble(6, ticket.getTargetLocation().getZ());
                    stmt.setFloat(7, ticket.getTargetRotation().getYaw());
                    stmt.setFloat(8, ticket.getTargetRotation().getPitch());
                    stmt.setLong(9, ticket.getCreateDate().getTime());

                    if (getType() == DatabaseType.MYSQL) {
                        stmt.setString(10, ticket.getTargetServer().getIdentifier());
                        stmt.setString(11, ticket.getTargetWorld().getIdentifier());
                        stmt.setDouble(12, ticket.getTargetLocation().getX());
                        stmt.setDouble(13, ticket.getTargetLocation().getY());
                        stmt.setDouble(14, ticket.getTargetLocation().getZ());
                        stmt.setFloat(15, ticket.getTargetRotation().getYaw());
                        stmt.setFloat(16, ticket.getTargetRotation().getPitch());
                        stmt.setLong(17, ticket.getCreateDate().getTime());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    public void clearTPTicketAsync(String uuid) {
        CompletableFuture.runAsync(() -> clearTPTicket(uuid).join());
    }

    public CompletableFuture<Void> clearTPTicket(String uuid) {
        return CompletableFuture.runAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.CLEAR_TP_TICKET, this.getConnectorSet());
            if (s1.isBlank()) return;

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    public CompletableFuture<Optional<TPTicket>> getTPTicket(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PULL_TP_TICKET, this.getConnectorSet());
            if (s1.isBlank()) return Optional.empty();

            AtomicReference<Optional<TPTicket>> ticket = new AtomicReference<>(Optional.empty());
            this.executeQuery(s1, stmt -> {
                try {
                    stmt.setString(1, uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, rs -> {
                try {
                    if (rs.next()) {
                        try {
                            String serverName = rs.getString("ServerName");
                            String worldName = rs.getString("WorldName");
                            double x = rs.getDouble("X");
                            double y = rs.getDouble("Y");
                            double z = rs.getDouble("Z");
                            float yaw = rs.getFloat("Yaw");
                            float pitch = rs.getFloat("Pitch");
                            long time = rs.getLong("PostDate");

                            CosmicServer server = new CosmicServer(serverName);
                            PlayerWorld world = new PlayerWorld(worldName);
                            WorldPosition position = new WorldPosition(x, y, z);
                            PlayerRotation rotation = new PlayerRotation(yaw, pitch);

                            Date d = new Date(time);

                            TPTicket t = new TPTicket(uuid, server, world, position, rotation, d);

                            ticket.set(Optional.of(t));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return ticket.get();
        });
    }

    public CompletableFuture<ConcurrentSkipListSet<TPTicket>> pullAllTPTickets() {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PULL_ALL_TP_TICKETS, this.getConnectorSet());
            if (s1.isBlank()) return new ConcurrentSkipListSet<>();

            ConcurrentSkipListSet<TPTicket> tickets = new ConcurrentSkipListSet<>();
            this.executeQuery(s1, stmt -> {}, rs -> {
                try {
                    while (rs.next()) {
                        try {
                            String uuid = rs.getString("Uuid");
                            String serverName = rs.getString("ServerName");
                            String worldName = rs.getString("WorldName");
                            double x = rs.getDouble("X");
                            double y = rs.getDouble("Y");
                            double z = rs.getDouble("Z");
                            float yaw = rs.getFloat("Yaw");
                            float pitch = rs.getFloat("Pitch");
                            long time = rs.getLong("PostDate");

                            CosmicServer server = new CosmicServer(serverName);
                            PlayerWorld world = new PlayerWorld(worldName);
                            WorldPosition position = new WorldPosition(x, y, z);
                            PlayerRotation rotation = new PlayerRotation(yaw, pitch);

                            Date d = new Date(time);

                            TPTicket t = new TPTicket(uuid, server, world, position, rotation, d);

                            tickets.add(t);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return tickets;
        });
    }
}
