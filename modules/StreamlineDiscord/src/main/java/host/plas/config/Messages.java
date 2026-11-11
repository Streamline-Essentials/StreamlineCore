package host.plas.config;

import host.plas.StreamlineDiscord;
import singularity.configs.ModularizedConfig;

/**
 * {@code messages.yml}: the plain text of every bot message, keyed like {@code messaging.yml}, plus the
 * Minecraft chat lines sent after verifying. A bot message uses this text when its
 * {@code messaging.yml} entry is {@code default}, and falls back to it when its embed cannot be read.
 */
public class Messages extends ModularizedConfig {
    public Messages() {
        super(StreamlineDiscord.getInstance(), "messages.yml", true);

        init();
    }

    @Override
    public void init() {
        verifySuccessMinecraft();
        unVerifySuccessMinecraft();
    }

    public String verifySuccessMinecraft() {
        reloadResource();

        return getOrSetDefault("verification.success.minecraft", "&a&lSuccess&8! &eYou verified &d%streamline_user_absolute% &eas &d%discord_user_name_tagged%");
    }

    public String unVerifySuccessMinecraft() {
        reloadResource();

        return getOrSetDefault("unverification.success.minecraft", "&a&lSuccess&8! &eYou unverified &d%streamline_user_absolute%");
    }

    /** The text stored under {@code key}, or null when there is none. */
    public synchronized String getText(String key) {
        reloadResource();

        Object value = getResource().get(key);
        if (value == null) return null;
        if (value instanceof java.util.List) {
            // A YAML list reads as one line per entry.
            StringBuilder builder = new StringBuilder();
            for (Object line : (java.util.List<?>) value) {
                if (builder.length() > 0) builder.append('\n');
                builder.append(line);
            }
            return builder.toString();
        }
        return value.toString();
    }

    /** Writes {@code text} under {@code key} unless the key already holds a value. */
    public synchronized void ensureDefault(String key, String text) {
        reloadResource();

        if (getResource().contains(key)) return;
        write(key, text);
    }

    public synchronized boolean has(String key) {
        reloadResource();

        return getResource().contains(key);
    }

    public synchronized void set(String key, Object value) {
        reloadResource();

        write(key, value);
    }

    public synchronized void remove(String key) {
        reloadResource();

        getResource().remove(key);
    }
}
