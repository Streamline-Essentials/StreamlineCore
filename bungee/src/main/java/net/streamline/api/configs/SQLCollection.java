package net.streamline.api.configs;

import java.util.TreeMap;

public class SQLCollection {
    public String collectionName;
    public TreeMap<String, Object> document;
    public String discriminatorKey;
    public Object discriminator;

    public SQLCollection(String collectionName, TreeMap<String, Object> document, String discriminatorKey, Object discriminator) {
        this.collectionName = collectionName;
        this.document = document;
        this.discriminatorKey = discriminatorKey;
        this.discriminator = discriminator;
    }

    public SQLCollection(String collectionName, String discriminatorKey, Object discriminator) {
        this(collectionName, new TreeMap<>(), discriminatorKey, discriminator);
    }

    public void putSet(String key, Object value) {
        this.document.put(key, value);
    }

    public <O> O getOrSetDefault(String key, O value) {
        try {
            O object = (O) this.document.get(key);
            if (object == null) this.putSet(key, value);

            return value;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
