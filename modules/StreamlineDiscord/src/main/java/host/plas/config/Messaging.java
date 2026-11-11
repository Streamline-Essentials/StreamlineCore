package host.plas.config;

import host.plas.StreamlineDiscord;
import singularity.configs.ModularizedConfig;

/**
 * {@code messaging.yml}: for each bot message, how it is sent. A value is {@code default} (the
 * plain text in {@code messages.yml}), the name of a file in {@code embeds/} ({@code .json}
 * optional), or {@code disabled} / {@code off} (send nothing).
 */
public class Messaging extends ModularizedConfig {
    public Messaging() {
        super(StreamlineDiscord.getInstance(), "messaging.yml", true);

        init();
    }

    @Override
    public void init() {
    }

    /**
     * The raw setting under {@code key}, or null when there is none. YAML reads an unquoted
     * {@code off} / {@code no} as {@code false}, so booleans come back as their string form.
     */
    public synchronized String getSetting(String key) {
        reloadResource();

        Object value = getResource().get(key);
        if (value == null) return null;
        return value.toString().trim();
    }

    /** Writes {@code setting} under {@code key} unless the key already holds a value. */
    public synchronized void ensureDefault(String key, String setting) {
        reloadResource();

        if (getResource().contains(key)) return;
        write(key, setting);
    }

    public synchronized void set(String key, String setting) {
        reloadResource();

        write(key, setting);
    }
}
