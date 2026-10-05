package gg.drak.tacoessentials.data;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.teleport.Teleports;
import singularity.configs.ModularizedConfig;
import singularity.data.players.location.RandomTeleportArea;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * {@code rtp.yml} in the module's folder: whether /rtp is on, and per world where it may land.
 * Values are re-read on every use, so edits apply without a restart.
 *
 * <p>The file is keyed by world name. A world without a section gets one with defaults the
 * first time it is asked for, picked by whether its name looks like a Nether or End.</p>
 */
public class RtpConfig extends ModularizedConfig {

    private static final String FILE = "rtp.yml";

    /**
     * Only the comment block at the top of the file survives the library rewriting the file,
     * so all of the documentation lives here.
     */
    private static final String HEADER = String.join("\n",
            "# Random teleport (/rtp) settings.",
            "#",
            "# 'enabled' at the top turns /rtp on or off everywhere. Every other top-level key is a",
            "# world, by the name the server uses for it: the folder name on Spigot/Paper, the",
            "# dimension id (such as minecraft:the_nether) on Fabric, Forge and NeoForge. A world",
            "# missing here gets a section with defaults the first time /rtp is used in it.",
            "#",
            "#   enabled         whether /rtp works in this world",
            "#   from            x and z of the center that distances are measured from",
            "#   min / max x, z  how far from the center a landing may be along each axis, as",
            "#                   absolute values: beyond min and within max",
            "#   min / max y     the lowest and highest feet height a landing may have; outside",
            "#                   the Nether, the surface is used whenever it lies in this range",
            "#   circular        true: min and max x/z are the radii of ellipses (circles when x",
            "#                   and z match); false: they are the half-widths of rectangles",
            "#   avoided-biomes  biome ids never landed in. '*' matches anything, so '*ocean*'",
            "#                   covers every ocean, and the minecraft: namespace may be left off",
            "#",
            "# Cooldown and attempts per use are in config.yml under rtp.",
            "");

    public RtpConfig() {
        super(TacoEssentials.getInstance(), writeTemplateIfMissing(), false);
        init();
    }

    /** Creates the file with its documentation header, which the library would not write. */
    private static String writeTemplateIfMissing() {
        File file = new File(TacoEssentials.getInstance().getDataFolder(), FILE);
        if (! file.exists()) {
            try {
                file.getParentFile().mkdirs();
                Files.write(file.toPath(), (HEADER + "\nenabled: true\n").getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                TacoEssentials.getInstance().logWarning("Could not write " + FILE + ": " + e.getMessage());
            }
        }
        return FILE;
    }

    /** Writes a section for every loaded world that lacks one. */
    @Override
    public void init() {
        isEnabled();
        for (String world : Teleports.gameplay().getWorldNames()) area(world);
    }

    /** Whether /rtp is enabled at all. */
    public boolean isEnabled() {
        reloadResource();
        return getOrSetDefault("enabled", true);
    }

    /**
     * The world's area, creating its section with defaults if it has none.
     *
     * @return the area, or empty when /rtp is disabled in that world
     */
    public Optional<RandomTeleportArea> area(String world) {
        reloadResource();
        Defaults d = Defaults.forWorld(world);
        String p = world + ".";
        if (! getOrSetDefault(p + "enabled", d.enabled)) return Optional.empty();
        List<String> biomes = getOrSetDefault(p + "avoided-biomes", new ArrayList<>(d.avoidedBiomes));
        return Optional.of(new RandomTeleportArea(world,
                getOrSetDefault(p + "from.x", 0), getOrSetDefault(p + "from.z", 0),
                getOrSetDefault(p + "min.x", d.minXz), getOrSetDefault(p + "min.z", d.minXz),
                getOrSetDefault(p + "max.x", d.maxXz), getOrSetDefault(p + "max.z", d.maxXz),
                getOrSetDefault(p + "min.y", d.minY), getOrSetDefault(p + "max.y", d.maxY),
                getOrSetDefault(p + "circular", false), biomes));
    }

    /** Starting values for a new world section. */
    private static final class Defaults {
        final boolean enabled;
        final int minXz;
        final int maxXz;
        final int minY;
        final int maxY;
        final List<String> avoidedBiomes;

        Defaults(boolean enabled, int minXz, int maxXz, int minY, int maxY, List<String> avoidedBiomes) {
            this.enabled = enabled;
            this.minXz = minXz;
            this.maxXz = maxXz;
            this.minY = minY;
            this.maxY = maxY;
            this.avoidedBiomes = avoidedBiomes;
        }

        static Defaults forWorld(String world) {
            String name = world.toLowerCase(Locale.ROOT);
            if (name.contains("nether")) {
                // Below the bedrock roof at 128.
                return new Defaults(true, 100, 2000, 32, 120, List.of());
            }
            if (name.endsWith("the_end") || name.endsWith("_end") || name.equals("end")) {
                // Off by default: most columns past the main island are void, so attempts run
                // out unless the area is tuned to where the outer islands are.
                return new Defaults(false, 1500, 5000, 40, 120, List.of("the_end", "small_end_islands"));
            }
            return new Defaults(true, 500, 10000, 60, 320, List.of("*ocean*", "*river*"));
        }
    }
}
