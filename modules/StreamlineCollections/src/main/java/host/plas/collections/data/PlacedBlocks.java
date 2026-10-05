package host.plas.collections.data;

import host.plas.collections.StreamlineCollections;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers which blocks players placed, so breaking them again does not count towards a
 * collection.
 *
 * <p>Positions are grouped into 512x512 regions, one file each under
 * {@code placed-blocks/<world>/}. A region is read the first time a block in it is placed or
 * broken, written back by {@link #flush}, and dropped from memory after ten idle minutes.</p>
 *
 * <p>Only placing and breaking are seen, on every platform alike: a placed block moved by a
 * piston, or removed by an explosion, fire or decay, keeps or loses its mark accordingly at its
 * old position.</p>
 */
public class PlacedBlocks {
    private static final long IDLE_MILLIS = 10L * 60L * 1000L;

    private final File folder;
    private final Map<String, Region> regions = new ConcurrentHashMap<>();

    private static final class Region {
        private final File file;
        private final Set<Long> positions = ConcurrentHashMap.newKeySet();
        private volatile boolean dirty;
        private volatile long lastUsed = System.currentTimeMillis();

        private Region(File file) {
            this.file = file;
        }
    }

    public PlacedBlocks(File dataFolder) {
        this.folder = new File(dataFolder, "placed-blocks");
    }

    public void mark(String world, int x, int y, int z) {
        Region region = region(world, x, z);
        if (region.positions.add(pack(x, y, z))) region.dirty = true;
    }

    /** Clears a mark, returning whether the block was marked. */
    public boolean clear(String world, int x, int y, int z) {
        Region region = region(world, x, z);
        boolean removed = region.positions.remove(pack(x, y, z));
        if (removed) region.dirty = true;
        return removed;
    }

    /**
     * Writes changed regions to disk and drops idle ones from memory; with {@code all}, every
     * written region is dropped, for shutdown.
     */
    public void flush(boolean all) {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Region> entry : regions.entrySet()) {
            Region region = entry.getValue();
            if (region.dirty) write(region);
            if (all || now - region.lastUsed > IDLE_MILLIS) {
                if (! region.dirty) regions.remove(entry.getKey(), region);
            }
        }
    }

    private Region region(String world, int x, int z) {
        int rx = x >> 9;
        int rz = z >> 9;
        String safeWorld = world == null ? "world" : world.replaceAll("[^A-Za-z0-9_.-]", "_");
        String key = safeWorld + "|" + rx + "|" + rz;

        Region region = regions.computeIfAbsent(key, k -> read(new File(new File(folder, safeWorld), "r." + rx + "." + rz + ".dat")));
        region.lastUsed = System.currentTimeMillis();
        return region;
    }

    private static Region read(File file) {
        Region region = new Region(file);
        if (! file.isFile()) return region;

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) region.positions.add(in.readLong());
        } catch (IOException e) {
            StreamlineCollections.getInstance().logWarning("Could not read placed blocks from " + file + ": " + e.getMessage());
        }
        return region;
    }

    private static void write(Region region) {
        region.dirty = false;
        List<Long> snapshot = new ArrayList<>(region.positions);

        try {
            File parent = region.file.getParentFile();
            if (parent != null && ! parent.isDirectory() && ! parent.mkdirs()) {
                throw new IOException("Could not create " + parent);
            }

            if (snapshot.isEmpty()) {
                Files.deleteIfExists(region.file.toPath());
                return;
            }

            // Written beside the region file and moved over it, so a crash mid-write leaves the old file.
            File temp = new File(region.file.getPath() + ".tmp");
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(temp)))) {
                out.writeInt(snapshot.size());
                for (long position : snapshot) out.writeLong(position);
            }
            Files.move(temp.toPath(), region.file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            region.dirty = true;
            StreamlineCollections.getInstance().logWarning("Could not write placed blocks to " + region.file + ": " + e.getMessage());
        }
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFFL);
    }
}
