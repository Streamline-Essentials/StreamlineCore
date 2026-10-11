package host.plas.hud;

import singularity.client.ClientInfo;

/**
 * Made-up values the HUD editor and the menu preview show for an element while there is no
 * world to read real ones from.
 */
final class SampleInfo implements ClientInfo {

    static final ClientInfo INSTANCE = new SampleInfo();

    private SampleInfo() {}

    public boolean inWorld() { return true; }
    public int fps() { return 144; }
    public String playerName() { return "Steve"; }
    public double x() { return 128.5; }
    public double y() { return 64.0; }
    public double z() { return -42.5; }
    public float yaw() { return -178.3f; }
    public float pitch() { return 12.5f; }
    public String facing() { return "north"; }
    public String biome() { return "minecraft:plains"; }
    public String dimension() { return "minecraft:overworld"; }
    public int ping() { return 42; }
    public String serverAddress() { return "play.example.net"; }
    public long dayTime() { return 12500; }
    public long day() { return 12; }
    public int screenWidth() { return 427; }
    public int screenHeight() { return 240; }
}
