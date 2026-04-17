package singularity.configs.given.punishments;

import gg.drak.thebase.objects.AtomicString;
import gg.drak.thebase.storage.datastores.SimpleJsonDatastore;
import singularity.Singularity;

import java.util.Date;

/**
 * JSON-backed datastore for persisting {@link CosmicPunishment} records.
 *
 * <p>Each punishment is stored under the path
 * {@code <punishedUUID>.<type>.<idHash>.<field>} within the
 * {@code punishments.json} file located in the plugin's data folder.</p>
 */
public class PunishmentsDocument extends SimpleJsonDatastore<CosmicPunishment> {

    /**
     * Constructs the datastore backed by {@code punishments.json} in the
     * Singularity plugin's data folder. The file is not self-contained
     * (not bundled as an internal resource).
     */
    public PunishmentsDocument() {
        super("punishments.json", Singularity.getInstance(), false);
    }

    /**
     * Writes all fields of the given punishment to the JSON store for the
     * first time, using the punishment's own UUID, type, and idHash as the
     * key path.
     *
     * @param cosmicPunishment the punishment to initialise in the store
     */
    @Override
    public void init(CosmicPunishment cosmicPunishment) {
        String keyPrefix = cosmicPunishment.getPunishedUUID() + "." + cosmicPunishment.getType().name() + "." + cosmicPunishment.getIdHash() + ".";
        write(keyPrefix + "punisher", cosmicPunishment.getPunisherUUID());
        write(keyPrefix + "reason", cosmicPunishment.getReason());
        write(keyPrefix + "timeToUnpunish", cosmicPunishment.getTimeToUnpunish().getTime());
    }

    /**
     * Persists the current state of an existing punishment back to the JSON store,
     * overwriting any previously stored values at the same key path.
     *
     * @param cosmicPunishment the punishment whose data should be saved
     */
    @Override
    public void save(CosmicPunishment cosmicPunishment) {
        String keyPrefix = cosmicPunishment.getPunishedUUID() + "." + cosmicPunishment.getType().name() + "." + cosmicPunishment.getIdHash() + ".";
        write(keyPrefix + "punisher", cosmicPunishment.getPunisherUUID());
        write(keyPrefix + "reason", cosmicPunishment.getReason());
        write(keyPrefix + "timeToUnpunish", cosmicPunishment.getTimeToUnpunish().getTime());
    }

    /**
     * Retrieves a {@link CosmicPunishment} by its {@code idHash}.
     * Locates the key prefix by scanning the JSON tree, then reconstructs
     * the punishment from the stored punisher UUID, reason, and expiry time.
     *
     * @param hash the {@code idHash} of the punishment to look up
     * @return the matching {@link CosmicPunishment}, or {@code null} if not found
     */
    @Override
    public CosmicPunishment get(String hash) {
        String keyPrefix = getKeyPrefix(hash);
        if (keyPrefix == null) return null;

        String punisherUUID = get(keyPrefix + "punisher", String.class);
        String reason = get(keyPrefix + "reason", String.class);
        long timeToUnpunish = get(keyPrefix + "timeToUnpunish", Long.class);

        return new CosmicPunishment(hash, punisherUUID, reason, PunishmentType.valueOf(hash.split("\\.")[1]), new Date(timeToUnpunish));
    }

    /**
     * Locates and returns the full dotted key path (including trailing dot) for
     * the punishment identified by the given {@code idHash}, by walking the
     * three-level JSON tree (UUID → type → idHash).
     *
     * @param hash the {@code idHash} to search for
     * @return the key prefix string ending with a dot, or {@code null} if not found
     */
    public String getKeyPrefix(String hash) {
        AtomicString keyPrefix = new AtomicString();

        singleLayerKeySet().forEach(key -> {
            singleLayerKeySet(key).forEach(key2 -> {
                singleLayerKeySet(key + "." + key2).forEach(key3 -> {
                    if (key3.equals(hash)) keyPrefix.set(key + "." + key2 + "." + key3 + ".");
                });
            });
        });

        return keyPrefix.get();
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
