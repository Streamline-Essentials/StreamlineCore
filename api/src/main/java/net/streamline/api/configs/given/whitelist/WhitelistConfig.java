package net.streamline.api.configs.given.whitelist;

import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.utils.MessageUtils;
import tv.quaint.storage.documents.SimpleJsonDocument;

import java.util.Date;

public class WhitelistConfig extends SimpleJsonDocument {
    public WhitelistConfig() {
        super("whitelist.json", SLAPI.getInstance().getDataFolder(), false);
    }

    public void setEnabled(boolean bool) {
        getResource().set("enabled", bool);
    }

    public boolean isEnabled() {
        reloadResource();

        return getResource().getOrDefault("enabled", false);
    }

    public void setEnforced(boolean bool) {
        getResource().set("enforced", bool);
    }

    public boolean isEnforced() {
        reloadResource();

        return getResource().getOrDefault("enforced", false);
    }

    public void addEntry(WhitelistEntry entry) {
        getResource().set("list." + entry.whitelistedAt().getTime() + ".uuid", entry.whitelistedUuid());
        getResource().set("list." + entry.whitelistedAt().getTime() + ".by", entry.whitelistedBy() != null ? entry.whitelistedBy() : GivenConfigs.getMainConfig().userConsoleDiscriminator());
    }

    public void removeEntry(WhitelistEntry entry) {
        getResource().remove("list." + entry.whitelistedAt().getTime() + ".uuid");
        getResource().remove("list." + entry.whitelistedAt().getTime() + ".by");
        getResource().remove("list." + entry.whitelistedAt().getTime());
    }

    public WhitelistEntry getEntry(String uuid) {
        reloadResource();

        for (String key : getResource().singleLayerKeySet("list")) {
            if (getResource().getString("list." + key + ".uuid").equals(uuid)) {
                try {
                    Date whitelistedAt = new Date(Long.parseLong(key));
                    String whitelistedUuid = getResource().getString("list." + key + ".uuid");
                    String whitelistedBy = getResource().getString("list." + key + ".by");

                    return new WhitelistEntry(whitelistedUuid, whitelistedAt, whitelistedBy);
                } catch (Exception e) {
                    MessageUtils.logWarning("Error getting WhitelistEntry for UUID of '" + uuid + "':");
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public boolean isEntryApplied(String uuid) {
        reloadResource();

        for (String key : getResource().singleLayerKeySet("list")) {
            if (getResource().getString("list." + key + ".uuid").equals(uuid)) return true;
        }
        return false;
    }

    @Override
    public void onInit() {

    }

    @Override
    public void onSave() {

    }
}
