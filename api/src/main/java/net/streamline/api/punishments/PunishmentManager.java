package net.streamline.api.punishments;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PunishmentManager {
    @Getter @Setter
    private static ConcurrentSkipListMap<Long, AbstractPunishment> punishments = new ConcurrentSkipListMap<>();

    public static <T extends AbstractPunishment> T addPunishment(T punishment) {
        getPunishments().put(punishment.getId(), punishment);
        return punishment;
    }

    public static <T extends AbstractPunishment> T removePunishment(T punishment) {
        return removePunishment(punishment.getId());
    }

    public static <T extends AbstractPunishment> T removePunishment(long id) {
        AbstractPunishment punishment = getPunishments().get(id);
        getPunishments().remove(id);
        return (T) punishment;
    }

    public static <T extends AbstractPunishment> T getPunishment(long id) {
        AbstractPunishment punishment = getPunishments().get(id);
        if (punishment == null) return null;
        return (T) punishment;
    }

    public static <T extends AbstractPunishment> ConcurrentSkipListMap<Long, T> filterPunishments(Predicate<AbstractPunishment> predicate) {
        List<AbstractPunishment> list = getPunishments().values().stream().filter(predicate).collect(Collectors.toList());

        ConcurrentSkipListMap<Long, T> r = new ConcurrentSkipListMap<>();

        for (AbstractPunishment punishment : list) {
            r.put(punishment.getId(), (T) punishment);
        }

        return r;
    }

    public static long nextId() {
        return getPunishments().lastKey() + 1L;
    }

    public static File getPunishmentFolder() {
        File r = new File(SLAPI.getInstance().getDataFolder(), "punishments" + File.separator);

        r.mkdirs();

        return r;
    }
}
