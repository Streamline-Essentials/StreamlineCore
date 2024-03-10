package net.streamline.api.database;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.uuid.UuidInfo;

public class CoreDBOperator extends DBOperator {
    public CoreDBOperator(ConnectorSet set) {
        super(set, "StreamlineCore");
    }

    public void ensureDatabase() {
        String s1 = Statements.getStatement(Statements.StatementType.CREATE_DATABASE, this.getConnectorSet());
        if (s1 == null) return;
        if (s1.isBlank() || s1.isEmpty()) return;

        this.execute(s1);
    }

    public void ensureTable() {
        String s1 = Statements.getStatement(Statements.StatementType.CREATE_TABLES, this.getConnectorSet());
        if (s1 == null) return;
        if (s1.isBlank() || s1.isEmpty()) return;

        this.execute(s1);
    }

    public void ensureUsable() {
        this.ensureFile();
        this.ensureDatabase();
        this.ensureTable();
    }

    public void savePlayer(StreamPlayer player, boolean async) {
        if (async) {
            CompletableFuture.runAsync(() -> savePlayer(player));
        } else {
            savePlayerMethod(player);
        }
    }

    public void savePlayer(StreamPlayer player) {
        savePlayer(player, true);
    }

    private void savePlayerMethod(StreamPlayer player) {
        ensureUsable();

        String s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_MAIN, this.getConnectorSet());
        if (s1 == null) return;
        if (s1.isBlank() || s1.isEmpty()) return;

        s1 = s1.replace("%uuid%", player.getUuid());
        s1 = s1.replace("%first_join%", String.valueOf(player.getFirstJoin().getTime()));
        s1 = s1.replace("%last_join%", String.valueOf(player.getLastJoin().getTime()));
        s1 = s1.replace("%current_name%", player.getCurrentName());
        s1 = s1.replace("%current_ip%", player.getCurrentIP());
        s1 = s1.replace("%play_seconds%", String.valueOf(player.getPlaySeconds()));
        s1 = s1.replace("%points%", String.valueOf(player.getPoints()));

        this.execute(s1);

        if (player.getMeta() != null) {
            s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_META, this.getConnectorSet());
            if (s1 == null) return;
            if (s1.isBlank() || s1.isEmpty()) return;

            s1 = s1.replace("%uuid%", player.getUuid());
            s1 = s1.replace("%nickname%", player.getMeta().getNickname());
            s1 = s1.replace("%prefix%", player.getMeta().getPrefix());
            s1 = s1.replace("%suffix%", player.getMeta().getSuffix());

            this.execute(s1);
        }

        if (player.getLeveling() != null) {
            s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_LEVELING, this.getConnectorSet());
            if (s1 == null) return;
            if (s1.isBlank() || s1.isEmpty()) return;

            s1 = s1.replace("%uuid%", player.getUuid());
            s1 = s1.replace("%level%", String.valueOf(player.getLeveling().getLevel()));
            s1 = s1.replace("%total_experience%", String.valueOf(player.getLeveling().getTotalExperience()));
            s1 = s1.replace("%current_experience%", String.valueOf(player.getLeveling().getCurrentExperience()));
            s1 = s1.replace("%equation_string%", player.getLeveling().getEquationString());
            s1 = s1.replace("%started_level%", String.valueOf(player.getLeveling().getStartedLevel()));
            s1 = s1.replace("%started_experience%", String.valueOf(player.getLeveling().getStartedExperience()));

            this.execute(s1);
        }

        if (player.getLocation() != null) {
            s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_LOCATION, this.getConnectorSet());
            if (s1 == null) return;
            if (s1.isBlank() || s1.isEmpty()) return;

            s1 = s1.replace("%uuid%", player.getUuid());
            s1 = s1.replace("%server%", player.getLocation().getServerName());
            s1 = s1.replace("%world%", player.getLocation().getWorldName());
            s1 = s1.replace("%x%", String.valueOf(player.getLocation().getX()));
            s1 = s1.replace("%y%", String.valueOf(player.getLocation().getY()));
            s1 = s1.replace("%z%", String.valueOf(player.getLocation().getZ()));
            s1 = s1.replace("%yaw%", String.valueOf(player.getLocation().getYaw()));
            s1 = s1.replace("%pitch%", String.valueOf(player.getLocation().getPitch()));

            this.execute(s1);
        }

        if (player.getPermissions() != null) {
            s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_PERMISSIONS, this.getConnectorSet());
            if (s1 == null) return;
            if (s1.isBlank() || s1.isEmpty()) return;

            s1 = s1.replace("%uuid%", player.getUuid());
            s1 = s1.replace("%bypassing_permissions%", String.valueOf(player.getPermissions().isBypassingPermissions()));

            this.execute(s1);
        }
    }

    public CompletableFuture<Optional<StreamPlayer>> loadPlayer(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            if (! exists(uuid).join()) return Optional.empty();

            StreamPlayer player = new StreamPlayer(uuid);

            String s1 = Statements.getStatement(Statements.StatementType.PULL_PLAYER_MAIN, this.getConnectorSet());
            if (s1 == null) return Optional.empty();
            if (s1.isBlank() || s1.isEmpty()) return Optional.empty();

            s1 = s1.replace("%uuid%", uuid);

            this.executeQuery(s1, rs -> {
                try {
                    if (rs.next()) {
                        player.setFirstJoin(rs.getLong("FirstJoin"));
                        player.setLastJoin(rs.getLong("LastJoin"));
                        player.setCurrentName(rs.getString("CurrentName"));
                        player.setCurrentIP(rs.getString("CurrentIP"));
                        player.setPlaySeconds(rs.getLong("PlaySeconds"));
                        player.setPoints(rs.getInt("Points"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            String s2 = Statements.getStatement(Statements.StatementType.PULL_PLAYER_META, this.getConnectorSet());
            if (s2 == null) return Optional.empty();
            if (s2.isBlank() || s2.isEmpty()) return Optional.empty();

            s2 = s2.replace("%uuid%", uuid);

            this.executeQuery(s2, rs -> {
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

            s3 = s3.replace("%uuid%", uuid);

            this.executeQuery(s3, rs -> {
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

            s4 = s4.replace("%uuid%", uuid);

            this.executeQuery(s4, rs -> {
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

            s5 = s5.replace("%uuid%", uuid);

            this.executeQuery(s5, rs -> {
                try {
                    if (rs.next()) {
                        player.getPermissions().setBypassingPermissions(rs.getBoolean("BypassingPermissions"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return Optional.of(player);
        });
    }

    public CompletableFuture<Boolean> exists(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PLAYER_EXISTS, this.getConnectorSet());
            if (s1 == null) return false;
            if (s1.isBlank() || s1.isEmpty()) return false;

            s1 = s1.replace("%uuid%", uuid);

            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            this.executeQuery(s1, rs -> {
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

            s1 = s1.replace("%uuid%", uuidInfo.getUuid().toString());
            s1 = s1.replace("%names%", uuidInfo.computableNames());
            s1 = s1.replace("%ips%", uuidInfo.computableIps());

            this.execute(s1);

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
            this.executeQuery(s1, rs -> {
                try {
                    if (rs.next()) {
                        String names = rs.getString("Names");
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
            this.executeQuery(s1, rs -> {
                try {
                    while (rs.next()) {
                        String uuid = rs.getString("Uuid");
                        String names = rs.getString("Names");
                        String ips = rs.getString("Ips");

                        UuidInfo info = new UuidInfo(uuid, names, ips);

                        uuids.get().add(info);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return uuids.get();
        });
    }
}
