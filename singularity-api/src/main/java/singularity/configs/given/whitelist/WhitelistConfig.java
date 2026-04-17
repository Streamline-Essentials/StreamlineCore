package singularity.configs.given.whitelist;

import gg.drak.thebase.storage.documents.SimpleJsonDocument;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.utils.MessageUtils;

import java.util.Date;

/**
 * JSON-backed configuration that manages the server whitelist.
 *
 * <p>Entries are stored in {@code whitelist.json} under the plugin's data
 * folder, keyed by the entry's creation timestamp (epoch millis). The
 * whitelist can be enabled independently of enforcement: when enforced,
 * players not on the list are rejected at login.</p>
 */
public class WhitelistConfig extends SimpleJsonDocument {

    /**
     * Constructs the whitelist configuration, backed by {@code whitelist.json}
     * in the plugin's data folder. The file is not self-contained (not bundled
     * as an internal resource).
     */
    public WhitelistConfig() {
        super("whitelist.json", Singularity.getInstance().getDataFolder(), false);
    }

    /**
     * Sets whether the whitelist feature is active.
     *
     * @param bool {@code true} to enable the whitelist; {@code false} to disable it
     */
    public void setEnabled(boolean bool) {
        getResource().set("enabled", bool);
    }

    /**
     * Returns whether the whitelist is currently enabled.
     * Reloads the backing resource before reading.
     *
     * @return {@code true} if the whitelist is enabled; {@code false} otherwise
     */
    public boolean isEnabled() {
        reloadResource();

        return getResource().getOrDefault("enabled", false);
    }

    /**
     * Sets whether the whitelist is enforced (i.e. non-whitelisted players are
     * blocked from joining).
     *
     * @param bool {@code true} to enforce the whitelist; {@code false} to allow all players
     */
    public void setEnforced(boolean bool) {
        getResource().set("enforced", bool);
    }

    /**
     * Returns whether the whitelist is currently being enforced.
     * Reloads the backing resource before reading.
     *
     * @return {@code true} if the whitelist is enforced; {@code false} otherwise
     */
    public boolean isEnforced() {
        reloadResource();

        return getResource().getOrDefault("enforced", false);
    }

    /**
     * Persists a {@link WhitelistEntry} to the JSON store, using the entry's
     * creation timestamp (epoch millis) as the key. If the entry has no
     * granting party, the console discriminator is used as a fallback.
     *
     * @param entry the whitelist entry to add
     */
    public void addEntry(WhitelistEntry entry) {
        getResource().set("list." + entry.whitelistedAt().getTime() + ".uuid", entry.whitelistedUuid());
        getResource().set("list." + entry.whitelistedAt().getTime() + ".by", entry.whitelistedBy() != null ? entry.whitelistedBy() : GivenConfigs.getMainConfig().getConsoleDiscriminator());
    }

    /**
     * Removes a {@link WhitelistEntry} from the JSON store, deleting all keys
     * associated with the entry's timestamp.
     *
     * @param entry the whitelist entry to remove
     */
    public void removeEntry(WhitelistEntry entry) {
        getResource().remove("list." + entry.whitelistedAt().getTime() + ".uuid");
        getResource().remove("list." + entry.whitelistedAt().getTime() + ".by");
        getResource().remove("list." + entry.whitelistedAt().getTime());
    }

    /**
     * Looks up and returns the {@link WhitelistEntry} for the given player UUID,
     * reloading the resource first. Returns {@code null} if no entry exists for
     * that UUID or if the stored timestamp cannot be parsed.
     *
     * @param uuid the player UUID to search for
     * @return the matching {@link WhitelistEntry}, or {@code null} if not found
     */
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

    /**
     * Returns whether the given player UUID has an active whitelist entry.
     * Reloads the backing resource before scanning.
     *
     * @param uuid the player UUID to check
     * @return {@code true} if the UUID is found in the whitelist; {@code false} otherwise
     */
    public boolean isEntryApplied(String uuid) {
        reloadResource();

        for (String key : getResource().singleLayerKeySet("list")) {
            if (getResource().getString("list." + key + ".uuid").equals(uuid)) return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInit() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSave() {

    }
}
