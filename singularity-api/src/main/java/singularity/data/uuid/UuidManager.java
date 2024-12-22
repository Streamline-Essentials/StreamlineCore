package singularity.data.uuid;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

@Getter @Setter
public class UuidManager {
    @Getter @Setter
    private static ConcurrentSkipListSet<UuidInfo> uuids = new ConcurrentSkipListSet<>();

    public static void registerUuid(UuidInfo uuidInfo) {
        if (uuids.contains(uuidInfo)) unregisterUuid(uuidInfo);
        uuids.add(uuidInfo);
    }

    public static Optional<UuidInfo> unregisterUuid(UuidInfo uuidInfo) {
        if (! uuids.contains(uuidInfo)) return Optional.empty();
        uuids.removeIf(uuidInfo1 -> uuidInfo1.getUuid().equals(uuidInfo.getUuid()));
        return Optional.of(uuidInfo);
    }

    public static Optional<UuidInfo> getUuid(String uuid) {
        return uuids.stream().filter(uuidInfo -> uuidInfo.getUuid().equals(uuid)).findFirst();
    }

    public static Optional<UuidInfo> unregister(String uuid) {
        AtomicReference<Optional<UuidInfo>> uuidInfo = new AtomicReference<>(Optional.empty());
        getUuid(uuid).ifPresent(u -> uuidInfo.set(unregisterUuid(u)));
        return uuidInfo.get();
    }

    public static void registerAll(ConcurrentSkipListSet<UuidInfo> set) {
        uuids.addAll(set);
    }

    public static void unregisterAll(ConcurrentSkipListSet<UuidInfo> set) {
        set.forEach(UuidManager::unregisterUuid);
    }

    public static void clear() {
        getUuids().forEach(ui -> {
            ui.save();
            ui.unregister();
        });
    }

    public static Optional<UuidInfo> getFromName(String name) {
        AtomicReference<Optional<UuidInfo>> uuidInfo = new AtomicReference<>(Optional.empty());

        getUuids().forEach(ui -> {
            if (ui.getNamesCaseInsensitive().contains(name.toLowerCase())) {
                uuidInfo.set(Optional.of(ui));
            }
        });

        return uuidInfo.get();
    }

    public static Optional<String> getUuidFromName(String name) {
        return getFromName(name).map(UuidInfo::getUuid);
    }

    public static String makeDashedUUID(String uuid) {
        if (uuid.length() != 32) return uuid;
        return uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20);
    }

    public static void cachePlayer(String uuid, String name, String ip) {
        CompletableFuture.runAsync(() -> {
            Optional<UuidInfo> infoOptional = getUuid(uuid);
            if (infoOptional.isEmpty()) {
                Optional<UuidInfo> optional = Singularity.getMainDatabase().loadUuidInfo(uuid).join();
                if (optional.isEmpty()) {
                    UuidInfo u = new UuidInfo(uuid, name, ip);
                    u.register();

                    u.save();
                } else {
                    UuidInfo u = optional.get();
                    u.register();

                    u.addName(name);
                    u.addIp(ip);

                    u.save();
                }
            } else {
                UuidInfo u = infoOptional.get();
                u.register();

                u.addName(name);
                u.addIp(ip);

                u.save();
            }
        });
    }
}
