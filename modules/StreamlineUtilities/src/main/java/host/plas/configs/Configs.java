package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.StreamlineUtilities;
import host.plas.essentials.configured.ConfiguredBlacklist;
import host.plas.essentials.configured.ConfiguredPermissionsList;

public class Configs extends SimpleConfiguration {
    public Configs() {
        super("config.yml", StreamlineUtilities.getInstance().getDataFolder(), true);
    }

    @Override
    public void init() {
        chatModifyEnabled();
        chatModifyPermission();
        isNicknamesEnabled();

        getTPATimeout();
        getTPADelayTicks();
        getTPABlacklist();

        lastServerEnabled();
        lastServerPermissionRequired();
        lastServerPermissionValue();
        lastServerDefaultServer();

        homesEnabled();
        homesDelayTicks();
        getHomesPermissions();
        getHomesBlacklist();
    }

    public boolean chatModifyEnabled() {
        reloadResource();

        return getResource().getOrSetDefault("chat.modify.modify", true);
    }

    public String chatModifyPermission() {
        reloadResource();

        return getResource().getOrSetDefault("chat.modify.permission", "streamline.utils.chat.modify");
    }

    public boolean isNicknamesEnabled() {
        reloadResource();

        return getResource().getOrSetDefault("nicknames.enabled", true);
    }

    public String getNicknamesFormat() {
        reloadResource();

        return getResource().getOrSetDefault("nicknames.format", "%this_input%");
    }

    public long getTPATimeout() {
        reloadResource();

        return getResource().getOrSetDefault("tpa.timeout", 600L);
    }

    public long getTPADelayTicks() {
        reloadResource();

        return getResource().getOrSetDefault("tpa.delay-ticks", 20L);
    }

    public ConfiguredBlacklist getTPABlacklist() {
        reloadResource();

        return new ConfiguredBlacklist(getResource().getSection("tpa.blacklist"));
    }

    public boolean lastServerEnabled() {
        reloadResource();

        return getResource().getOrSetDefault("last-server.enabled", false);
    }

    public boolean lastServerPermissionRequired() {
        reloadResource();

        return getResource().getOrSetDefault("last-server.permission.required", true);
    }

    public String lastServerPermissionValue() {
        reloadResource();

        return getResource().getOrSetDefault("last-server.permission.value", "streamline.utils.last-server");
    }

    public String lastServerDefaultServer() {
        reloadResource();

        return getResource().getOrSetDefault("last-server.default-server", "hub");
    }

    public boolean homesEnabled() {
        reloadResource();

        return getResource().getOrSetDefault("homes.enabled", true);
    }

    public int homesDelayTicks() {
        reloadResource();

        return getResource().getOrSetDefault("homes.delay-ticks", 20);
    }

    public ConfiguredPermissionsList getHomesPermissions() {
        reloadResource();

        return new ConfiguredPermissionsList(getResource().getSection("homes.permissions"));
    }

    public ConfiguredBlacklist getHomesBlacklist() {
        reloadResource();

        return new ConfiguredBlacklist(getResource().getSection("homes.blacklist"));
    }
}
