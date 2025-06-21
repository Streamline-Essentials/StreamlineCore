package singularity.configs.given.whitelist;

import gg.drak.thebase.storage.documents.SimpleJsonDocument;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.utils.MessageUtils;

import java.util.Date;

public class WhitelistConfig extends SimpleJsonDocument {
    public WhitelistConfig() {
        super("whitelist.json", Singularity.getInstance().getDataFolder(), false);
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
        getResource().set("list." + entry.whitelistedAt().getTime() + ".by", entry.whitelistedBy() != null ? entry.whitelistedBy() : GivenConfigs.getMainConfig().getConsoleDiscriminator());
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
