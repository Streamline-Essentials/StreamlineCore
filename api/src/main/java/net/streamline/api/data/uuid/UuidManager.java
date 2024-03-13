package net.streamline.api.data.uuid;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

@Getter @Setter
public class UuidManager {
    @Getter @Setter
    private static ConcurrentSkipListSet<UuidInfo> uuids = new ConcurrentSkipListSet<>();

    public static Optional<UuidInfo> registerUuid(UuidInfo uuidInfo) {
        if (uuids.contains(uuidInfo)) {
            return Optional.empty();
        }
        uuids.add(uuidInfo);
        return Optional.of(uuidInfo);
    }

    public static Optional<UuidInfo> unregisterUuid(UuidInfo uuidInfo) {
        if (! uuids.contains(uuidInfo)) {
            return Optional.empty();
        }
        uuids.removeIf(uuidInfo1 -> uuidInfo1.getUuid().equals(uuidInfo.getUuid()));
        return Optional.of(uuidInfo);
    }

    public static Optional<UuidInfo> getUuid(String uuid) {
        return uuids.stream().filter(uuidInfo -> uuidInfo.getUuid().toString().equals(uuid)).findFirst();
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
            if (ui.getNames().contains(name)) {
                uuidInfo.set(Optional.of(ui));
            }
        });

        return uuidInfo.get();
    }

    public static Optional<String> getUuidFromName(String name) {
        return getFromName(name).map(UuidInfo::getUuid).map(Object::toString);
    }

    public static String makeDashedUUID(String uuid) {
        if (uuid.length() != 32) return uuid;
        return uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20);
    }

    public static void cachePlayer(String uuid, String name, String ip) {
        Optional<UuidInfo> infoOptional = getUuid(uuid);
        if (infoOptional.isEmpty()) {
            SLAPI.getMainDatabase().loadUuidInfo(uuid).whenComplete((uuidInfo, throwable) -> {
                if (throwable != null) throwable.printStackTrace();

                if (uuidInfo.isEmpty()) {
                    UuidInfo info = new UuidInfo(uuid, name, ip);
                    info.register();
                } else {
                    UuidInfo u = uuidInfo.get();
                    u.register();

                    u.addName(name);
                    u.addIp(ip);

                    u.save();
                }
            });
        } else {
            UuidInfo u = infoOptional.get();
            u.register();

            u.addName(name);
            u.addIp(ip);

            u.save();
        }
    }
}
