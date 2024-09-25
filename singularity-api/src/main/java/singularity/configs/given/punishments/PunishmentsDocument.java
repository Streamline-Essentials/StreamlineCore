package singularity.configs.given.punishments;

import singularity.Singularity;
import tv.quaint.objects.AtomicString;
import tv.quaint.storage.datastores.SimpleJsonDatastore;

import java.util.Date;

public class PunishmentsDocument extends SimpleJsonDatastore<CosmicPunishment> {
    public PunishmentsDocument() {
        super("punishments.json", Singularity.getInstance(), false);
    }

    @Override
    public void init(CosmicPunishment cosmicPunishment) {
        String keyPrefix = cosmicPunishment.getPunishedUUID() + "." + cosmicPunishment.getType().name() + "." + cosmicPunishment.getIdHash() + ".";
        write(keyPrefix + "punisher", cosmicPunishment.getPunisherUUID());
        write(keyPrefix + "reason", cosmicPunishment.getReason());
        write(keyPrefix + "timeToUnpunish", cosmicPunishment.getTimeToUnpunish().getTime());
    }

    @Override
    public void save(CosmicPunishment cosmicPunishment) {
        String keyPrefix = cosmicPunishment.getPunishedUUID() + "." + cosmicPunishment.getType().name() + "." + cosmicPunishment.getIdHash() + ".";
        write(keyPrefix + "punisher", cosmicPunishment.getPunisherUUID());
        write(keyPrefix + "reason", cosmicPunishment.getReason());
        write(keyPrefix + "timeToUnpunish", cosmicPunishment.getTimeToUnpunish().getTime());
    }

    @Override
    public CosmicPunishment get(String hash) {
        String keyPrefix = getKeyPrefix(hash);
        if (keyPrefix == null) return null;

        String punisherUUID = get(keyPrefix + "punisher", String.class);
        String reason = get(keyPrefix + "reason", String.class);
        long timeToUnpunish = get(keyPrefix + "timeToUnpunish", Long.class);

        return new CosmicPunishment(hash, punisherUUID, reason, PunishmentType.valueOf(hash.split("\\.")[1]), new Date(timeToUnpunish));
    }

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

    @Override
    public void onInit() {

    }

    @Override
    public void onSave() {

    }
}
