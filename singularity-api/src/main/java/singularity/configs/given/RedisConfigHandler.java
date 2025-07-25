package singularity.configs.given;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;

@Getter @Setter
public class RedisConfigHandler extends SimpleConfiguration {
    public RedisConfigHandler() {
        super("redis-config.yml", Singularity.getInstance(), true);
    }

    @Override
    public void init() {
        getHost();
        getPort();
        getUsername();
        getPassword();
    }

    // DATABASE

    public String getHost() {
        reloadResource();

        return getResource().getOrSetDefault("host", "localhost");
    }

    public int getPort() {
        reloadResource();

        return getResource().getOrSetDefault("port", 6379);
    }

    public String getUsername() {
        reloadResource();

        return getResource().getOrSetDefault("username", "default");
    }

    public String getPassword() {
        reloadResource();

        return getResource().getOrSetDefault("password", "password");
    }

    public boolean isEnabled() {
        reloadResource();

        return getResource().getOrSetDefault("enabled", false);
    }
}
