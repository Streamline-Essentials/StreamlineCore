package net.streamline.api.text;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.utils.MessageUtils;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

public class DataComponent extends SLComponent {
    public static final String DATA_TYPE_KEY = "DATA_TYPE";

    @Getter @Setter
    private ConcurrentSkipListSet<DataPart> dataParts;

    public DataComponent(String raw, int substringStart, String main) {
        super(raw, substringStart, main);

        this.dataParts = DataPart.fromRaw(main);
    }

    public DataComponent(String raw, String main) {
        this(raw, 0, main);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        this.dataParts.forEach(dataPart -> {
            builder.append(" Parts: [ Whole: ");
            builder.append(dataPart.getRaw());
            builder.append(", Key: ");
            builder.append(dataPart.getKey());
            builder.append(", Value: ");
            builder.append(dataPart.getValue());
            builder.append(" ]");
        });

        return builder.toString();
    }

    public DataPart getPart(String part) {
        AtomicReference<DataPart> r = new AtomicReference<>(null);

        dataParts.forEach(dataPart -> {
            if (dataPart.getKey().equals(part)) {
                r.set(dataPart);
            }
        });

        return r.get();
    }

    public DataPart getPartAnyCase(String part) {
        AtomicReference<DataPart> r = new AtomicReference<>(null);

        dataParts.forEach(dataPart -> {
            if (dataPart.getKey().equalsIgnoreCase(part)) {
                r.set(dataPart);
            }
        });

        return r.get();
    }

    public void addPart(DataPart part) {
        dataParts.add(part);

        this.setMain(parseNewMain());
        this.setRaw(parseNewRaw());
    }

    public void addPart(String key, String value) {
        addPart(new DataPart(key, value));
    }

    public void removePart(String key) {
        DataPart part = getPart(key);

        if (part != null) {
            dataParts.remove(part);

            this.setMain(parseNewMain());
            this.setRaw(parseNewRaw());
        }
    }

    public void removePartAnyCase(String key) {
        DataPart part = getPartAnyCase(key);

        if (part != null) {
            dataParts.remove(part);

            this.setMain(parseNewMain());
            this.setRaw(parseNewRaw());
        }
    }

    public String parseNewMain() {
        StringBuilder builder = new StringBuilder();

        dataParts.forEach(dataPart -> {
            builder.append(dataPart.getRaw());
        });

        return builder.toString();
    }

    public String parseNewRaw() {
        return "{{-" + parseNewMain() + "-}}";
    }

    public boolean hasPart(String part) {
        return getPart(part) != null;
    }

    public boolean hasPartAnyCase(String part) {
        return getPartAnyCase(part) != null;
    }
}
