package gg.drak.tacoessentials.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player state consulted on hot paths (every chat message and command), loaded from the
 * database at login so those paths never touch the database. Writes go to both this cache
 * and the database.
 */
public final class Sessions {

    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    private Sessions() {}

    public static final class Session {
        private volatile boolean muted;
        private volatile boolean fly;
        private volatile boolean god;
        private volatile long lastRtpMillis;

        public boolean isGod() {
            return god;
        }

        public boolean isMuted() {
            return muted;
        }

        public boolean isFly() {
            return fly;
        }

        public long getLastRtpMillis() {
            return lastRtpMillis;
        }

        public void setLastRtpMillis(long millis) {
            lastRtpMillis = millis;
        }
    }

    public static Session load(String uuid) {
        Session session = new Session();
        TacoDatabase.player(uuid).ifPresent(record -> {
            session.muted = record.isMuted();
            session.fly = record.isFly();
        });
        session.god = TacoDatabase.isGod(uuid);
        SESSIONS.put(uuid, session);
        return session;
    }

    public static void unload(String uuid) {
        SESSIONS.remove(uuid);
    }

    public static void clear() {
        SESSIONS.clear();
    }

    /** The session of an online player; an empty default if they have none yet (never null). */
    public static Session get(String uuid) {
        return SESSIONS.computeIfAbsent(uuid, u -> new Session());
    }

    public static boolean isMuted(String uuid) {
        Session session = SESSIONS.get(uuid);
        return session != null && session.muted;
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
}
