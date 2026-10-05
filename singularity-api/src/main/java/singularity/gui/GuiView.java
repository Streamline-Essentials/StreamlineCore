package singularity.gui;

import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * What a renderer needs to show a {@link CosmicGui}: its id, title, size, items, and which slots
 * should report clicks. Holds no handlers, so it can travel between servers.
 *
 * <p>{@link #encode()} produces a compact string — binary, gzip, then unpadded URL-safe Base64 —
 * that fits a single {@code ProxiedMessage} value: it contains no {@code =} or {@code ;}, which
 * that message's {@code key=value;} wire format cannot carry.</p>
 */
@Getter @Setter
public class GuiView {
    private static final int FORMAT_VERSION = 1;

    private String id;
    private String title;
    private int rows;
    private ConcurrentSkipListMap<Integer, CosmicItem> items;
    private ConcurrentSkipListSet<Integer> clickable;

    public GuiView(String id, String title, int rows) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.rows = CosmicGui.clampRows(rows);
        this.items = new ConcurrentSkipListMap<>();
        this.clickable = new ConcurrentSkipListSet<>();
    }

    public int getSize() {
        return rows * 9;
    }

    public boolean isClickable(int slot) {
        return clickable.contains(slot);
    }

    /** Whether a renderer can swap this view's items into an open screen showing {@code other}. */
    public boolean isLayoutCompatible(GuiView other) {
        return other != null && other.rows == rows && other.title.equals(title);
    }

    public String encode() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
                out.writeByte(FORMAT_VERSION);
                out.writeUTF(id);
                out.writeUTF(title);
                out.writeByte(rows);
                out.writeShort(items.size());
                for (Map.Entry<Integer, CosmicItem> entry : items.entrySet()) {
                    out.writeByte(entry.getKey());
                    out.writeBoolean(clickable.contains(entry.getKey()));
                    entry.getValue().write(out);
                }
                // Clickable slots holding no item still report clicks.
                int bare = 0;
                for (int slot : clickable) if (! items.containsKey(slot)) bare++;
                out.writeShort(bare);
                for (int slot : clickable) if (! items.containsKey(slot)) out.writeByte(slot);
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Could not encode GUI " + id, e);
        }
    }

    public static GuiView decode(String encoded) throws IOException {
        byte[] raw = Base64.getUrlDecoder().decode(encoded);
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(raw)))) {
            int version = in.readByte();
            if (version != FORMAT_VERSION) throw new IOException("Unsupported GUI format version " + version);
            GuiView view = new GuiView(in.readUTF(), in.readUTF(), in.readByte());
            int count = in.readShort();
            for (int i = 0; i < count; i++) {
                int slot = in.readUnsignedByte();
                boolean clickable = in.readBoolean();
                view.items.put(slot, CosmicItem.read(in));
                if (clickable) view.clickable.add(slot);
            }
            int bare = in.readShort();
            for (int i = 0; i < bare; i++) view.clickable.add(in.readUnsignedByte());
            return view;
        }
    }
}
