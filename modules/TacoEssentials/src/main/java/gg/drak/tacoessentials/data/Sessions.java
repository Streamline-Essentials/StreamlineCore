package gg.drak.tacoessentials.data;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player state consulted on hot paths (every chat message and command), loaded from the
 * database at login so those paths never touch the database. Writes go to both this cache
 * and the database. AFK and activity are kept in memory only.
 */
public final class Sessions {

    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    private Sessions() {}

    public static final class Session {
        private volatile boolean muted;
        private volatile boolean fly;
        private volatile boolean god;
        private volatile boolean tpAuto;
        private volatile boolean frozen;
        /** Where a frozen player is held; set when the freeze starts or at login. */
        private volatile Loc frozenAt;
        private volatile long lastRtpMillis;
        private volatile boolean afk;
        private volatile long lastActivityMillis = System.currentTimeMillis();
        /** Where the AFK check last saw the player, to notice them moving. */
        private volatile Loc lastSeen;

        public boolean isGod() {
            return god;
        }

        public boolean isMuted() {
            return muted;
        }

        public boolean isFly() {
            return fly;
        }

        public boolean isTpAuto() {
            return tpAuto;
        }

        public boolean isFrozen() {
            return frozen;
        }

        public Loc getFrozenAt() {
            return frozenAt;
        }

        public void setFrozenAt(Loc loc) {
            frozenAt = loc;
        }

        public long getLastRtpMillis() {
            return lastRtpMillis;
        }

        public void setLastRtpMillis(long millis) {
            lastRtpMillis = millis;
        }

        public boolean isAfk() {
            return afk;
        }

        public void setAfk(boolean afk) {
            this.afk = afk;
        }

        public long getLastActivityMillis() {
            return lastActivityMillis;
        }

        public void markActive() {
            lastActivityMillis = System.currentTimeMillis();
        }

        public Loc getLastSeen() {
            return lastSeen;
        }

        public void setLastSeen(Loc loc) {
            lastSeen = loc;
        }
    }

    public static Session load(String uuid) {
        Session session = new Session();
        TacoDatabase.player(uuid).ifPresent(record -> {
            session.muted = record.isMuted();
            session.fly = record.isFly();
        });
        session.god = TacoDatabase.isGod(uuid);
        session.tpAuto = TacoDatabase.hasFlag(uuid, TacoDatabase.FLAG_TPAUTO);
        session.frozen = TacoDatabase.hasFlag(uuid, TacoDatabase.FLAG_FROZEN);
        SESSIONS.put(uuid, session);
        return session;
    }

    public static void unload(String uuid) {
        SESSIONS.remove(uuid);
    }

    public static void clear() {
        SESSIONS.clear();
    }

    /** The UUIDs of players with a session, i.e. those online. */
    public static Set<String> online() {
        return SESSIONS.keySet();
    }

    /** The session of an online player; an empty default if they have none yet (never null). */
    public static Session get(String uuid) {
        return SESSIONS.computeIfAbsent(uuid, u -> new Session());
    }

    public static boolean isMuted(String uuid) {
        Session session = SESSIONS.get(uuid);
        return session != null && session.muted;
    }

    public static boolean isFrozen(String uuid) {
        Session session = SESSIONS.get(uuid);
        return session != null && session.frozen;
    }

    public static void setMuted(String uuid, boolean muted) {
        TacoDatabase.setMuted(uuid, muted);
        Session session = SESSIONS.get(uuid);
        if (session != null) session.muted = muted;
    }

    public static void setGod(String uuid, boolean god) {
        TacoDatabase.setGod(uuid, god);
        Session session = SESSIONS.get(uuid);
        if (session != null) session.god = god;
    }

    public static void setFly(String uuid, boolean fly) {
        TacoDatabase.setFly(uuid, fly);
        Session session = SESSIONS.get(uuid);
        if (session != null) session.fly = fly;
    }

    public static void setTpAuto(String uuid, boolean on) {
        TacoDatabase.setFlag(uuid, TacoDatabase.FLAG_TPAUTO, on);
        Session session = SESSIONS.get(uuid);
        if (session != null) session.tpAuto = on;
    }

    /** @param at where an online player is held, or {@code null} while offline (set at login) */
    public static void setFrozen(String uuid, boolean frozen, Loc at) {
        TacoDatabase.setFlag(uuid, TacoDatabase.FLAG_FROZEN, frozen);
        Session session = SESSIONS.get(uuid);
        if (session == null) return;
        session.frozen = frozen;
        session.frozenAt = frozen ? at : null;
    }
}
