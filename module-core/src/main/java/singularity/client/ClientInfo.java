package singularity.client;

/**
 * What a game client knows about itself, for HUD elements and client modules. Every value
 * is read live on the render thread; outside a world the position values are 0, the names
 * are empty and {@link #inWorld()} is {@code false}.
 */
public interface ClientInfo {

    /** @return whether the client is in a world (singleplayer or connected to a server) */
    boolean inWorld();

    /** @return frames rendered over the last second */
    int fps();

    /** @return the local player's name, or an empty string */
    String playerName();

    double x();

    double y();

    double z();

    /** @return the yaw, wrapped to -180..180 */
    float yaw();

    float pitch();

    /** @return the horizontal facing, lowercase: {@code north}, {@code east}, {@code south}, {@code west} */
    String facing();

    /** @return the biome id at the player's feet, such as {@code minecraft:plains}, or an empty string */
    String biome();

    /** @return the dimension id, such as {@code minecraft:overworld}, or an empty string */
    String dimension();

    /** @return the round trip to the server in milliseconds, or -1 when unknown */
    int ping();

    /** @return the address of the server, {@code singleplayer} in a local world, or an empty string */
    String serverAddress();

    /** @return the day time of the world in ticks, 0..23999 */
    long dayTime();

    /** @return the number of in-game days since the world started */
    long day();

    /** @return the GUI-scaled screen width */
    int screenWidth();

    /** @return the GUI-scaled screen height */
    int screenHeight();

    /** Values for a client outside any world. */
    ClientInfo NONE = new ClientInfo() {
        public boolean inWorld() { return false; }
        public int fps() { return 0; }
        public String playerName() { return ""; }
        public double x() { return 0; }
        public double y() { return 0; }
        public double z() { return 0; }
        public float yaw() { return 0; }
        public float pitch() { return 0; }
        public String facing() { return ""; }
        public String biome() { return ""; }
        public String dimension() { return ""; }
        public int ping() { return -1; }
        public String serverAddress() { return ""; }
        public long dayTime() { return 0; }
        public long day() { return 0; }
        public int screenWidth() { return 0; }
        public int screenHeight() { return 0; }
    };
}
