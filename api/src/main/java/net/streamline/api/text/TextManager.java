package net.streamline.api.text;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ConcurrentSkipListSet;

public class TextManager {
    @Getter @Setter
    private static ConcurrentSkipListSet<HexResulter> hexResulters = new ConcurrentSkipListSet<>();

    public static void registerHexResulter(HexResulter resulter) {
        hexResulters.add(resulter);
    }

    public static void registerHexResulter(String starter, String ender) {
        registerHexResulter(new HexResulter(starter, ender));
    }

    public static void unregisterHexResulter(HexResulter resulter) {
        unregisterHexResulter(resulter.getIdentifiably());
    }

    public static void unregisterHexResulter(String identifiably) {
        hexResulters.removeIf(resulter -> resulter.getIdentifiably().equals(identifiably));
    }
}
