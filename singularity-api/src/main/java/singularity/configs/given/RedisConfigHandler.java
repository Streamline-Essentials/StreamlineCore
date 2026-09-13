package singularity.configs.given;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;

/**
 * Configuration handler for Redis connection settings, backed by
 * {@code redis-config.yml} in the plugin's data folder.
 *
 * <p>Calling {@link #init()} triggers each getter so that all keys are
 * written with their defaults on first load.</p>
 */
@Getter @Setter
public class RedisConfigHandler extends SimpleConfiguration {

    /**
     * Constructs the handler, loading (or creating) {@code redis-config.yml}
     * from the Singularity plugin's data folder as a self-contained resource.
     */
    public RedisConfigHandler() {
        super("redis-config.yml", Singularity.getInstance(), true);
    }

    /**
     * Eagerly reads every Redis setting so that default values are written to
     * the file when it is first created.
     */
    @Override
    public void init() {
        getHost();
        getPort();
        getUsername();
        getPassword();
        getPrefix();

        isEnabled();
    }

    /**
     * Returns the Redis server hostname, defaulting to {@code "localhost"}.
     * Reloads the resource before reading.
     *
     * @return the configured Redis host
     */
    public String getHost() {
        reloadResource();

        return getResource().getOrSetDefault("host", "localhost");
    }

    /**
     * Returns the Redis server port, defaulting to {@code 6379}.
     * Reloads the resource before reading.
     *
     * @return the configured Redis port
     */
    public int getPort() {
        reloadResource();

        return getResource().getOrSetDefault("port", 6379);
    }

    /**
     * Returns the Redis authentication username, defaulting to {@code "default"}.
     * Reloads the resource before reading.
     *
     * @return the configured Redis username
     */
    public String getUsername() {
        reloadResource();

        return getResource().getOrSetDefault("username", "default");
    }

    /**
     * Returns the Redis authentication password, defaulting to {@code "password"}.
     * Reloads the resource before reading.
     *
     * @return the configured Redis password
     */
    public String getPassword() {
        reloadResource();

        return getResource().getOrSetDefault("password", "password");
    }

    /**
     * Returns the key prefix used to namespace Redis keys, defaulting to
     * {@code "STREAMLINE:INSTANCE:"}. Reloads the resource before reading.
     *
     * @return the configured key prefix string
     */
    public String getPrefix() {
        reloadResource();

        return getResource().getOrSetDefault("prefix", "STREAMLINE:INSTANCE:");
    }

    /**
     * Returns whether the Redis integration is active, defaulting to {@code false}.
     * Reloads the resource before reading.
     *
     * @return {@code true} if Redis is enabled; {@code false} otherwise
     */
    public boolean isEnabled() {
        reloadResource();

        return getResource().getOrSetDefault("enabled", false);
    }
}
