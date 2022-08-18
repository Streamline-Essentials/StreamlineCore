package net.streamline.api.configs.given.whitelist;

import de.leonhard.storage.Json;
import de.leonhard.storage.sections.FlatFileSection;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.FlatFileResource;

import java.util.Date;

public class WhitelistConfig extends FlatFileResource<Json> {
    public WhitelistConfig() {
        super(Json.class, "whitelist.json", SLAPI.getInstance().getDataFolder(), false);
    }

    public void setEnabled(boolean bool) {
        resource.set("enabled", bool);
    }

    public boolean isEnabled() {
        reloadResource();

        return resource.getOrDefault("enabled", false);
    }

    public void setEnforced(boolean bool) {
        resource.set("enforced", bool);
    }

    public boolean isEnforced() {
        reloadResource();

        return resource.getOrDefault("enforced", false);
    }

    public void addEntry(WhitelistEntry entry) {
        resource.set("list." + entry.whitelistedAt().getTime() + ".uuid", entry.whitelistedUuid());
        resource.set("list." + entry.whitelistedAt().getTime() + ".by", entry.whitelistedBy() != null ? entry.whitelistedBy() : SLAPI.getInstance().getPlatform().getMainConfig().userConsoleDiscriminator());
    }

    public void removeEntry(WhitelistEntry entry) {
        resource.remove("list." + entry.whitelistedAt().getTime() + ".uuid");
        resource.remove("list." + entry.whitelistedAt().getTime() + ".by");
        resource.remove("list." + entry.whitelistedAt().getTime());
    }

    public WhitelistEntry getEntry(String uuid) {
        reloadResource();

        for (String key : resource.singleLayerKeySet("list")) {
            if (resource.getString("list." + key + ".uuid").equals(uuid)) {
                try {
                    Date whitelistedAt = new Date(Long.parseLong(key));
                    String whitelistedUuid = resource.getString("list." + key + ".uuid");
                    String whitelistedBy = resource.getString("list." + key + ".by");

                    return new WhitelistEntry(whitelistedUuid, whitelistedAt, whitelistedBy);
                } catch (Exception e) {
                    SLAPI.getInstance().getMessenger().logWarning("Error getting WhitelistEntry for UUID of '" + uuid + "':");
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public boolean isEntryApplied(String uuid) {
        reloadResource();

        for (String key : resource.singleLayerKeySet("list")) {
            if (resource.getString("list." + key + ".uuid").equals(uuid)) return true;
        }
        return false;
    }
}
