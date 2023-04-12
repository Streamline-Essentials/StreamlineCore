package net.streamline.api.configs.given.punishments;

import net.streamline.api.SLAPI;
import net.streamline.api.objects.AtomicString;
import tv.quaint.storage.datastores.SimpleJsonDatastore;
import tv.quaint.storage.documents.SimpleJsonDocument;

import java.util.Date;

public class PunishmentsDocument extends SimpleJsonDatastore<StreamlinePunishment> {
    public PunishmentsDocument() {
        super("punishments.json", SLAPI.getInstance(), false);
    }

    @Override
    public void init(StreamlinePunishment streamlinePunishment) {
        String keyPrefix = streamlinePunishment.getPunishedUUID() + "." + streamlinePunishment.getType().name() + "." + streamlinePunishment.getIdHash() + ".";
        write(keyPrefix + "punisher", streamlinePunishment.getPunisherUUID());
        write(keyPrefix + "reason", streamlinePunishment.getReason());
        write(keyPrefix + "timeToUnpunish", streamlinePunishment.getTimeToUnpunish().getTime());
    }

    @Override
    public void save(StreamlinePunishment streamlinePunishment) {
        String keyPrefix = streamlinePunishment.getPunishedUUID() + "." + streamlinePunishment.getType().name() + "." + streamlinePunishment.getIdHash() + ".";
        write(keyPrefix + "punisher", streamlinePunishment.getPunisherUUID());
        write(keyPrefix + "reason", streamlinePunishment.getReason());
        write(keyPrefix + "timeToUnpunish", streamlinePunishment.getTimeToUnpunish().getTime());
    }

    @Override
    public StreamlinePunishment get(String hash) {
        String keyPrefix = getKeyPrefix(hash);
        if (keyPrefix == null) return null;

        String punisherUUID = get(keyPrefix + "punisher", String.class);
        String reason = get(keyPrefix + "reason", String.class);
        long timeToUnpunish = get(keyPrefix + "timeToUnpunish", Long.class);

        StreamlinePunishment punishment = new StreamlinePunishment(hash, punisherUUID, reason, PunishmentType.valueOf(hash.split("\\.")[1]), new Date(timeToUnpunish));

        return punishment;
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
