package net.streamline.api.holders;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.modules.ModuleUtils;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class StreamlineDependencyHolder<T> {
    @Getter @Setter
    private ConcurrentSkipListMap<Integer, String> keysToTry;
    @Getter @Setter
    private T api;
    @Getter @Setter
    private String identifier;

    StreamlineDependencyHolder(String identifier, String... keysToTry) {
        this.identifier = identifier;
        this.keysToTry = new ConcurrentSkipListMap<>();
        for (String key : keysToTry) {
            this.keysToTry.put(this.keysToTry.size(), key);
        }
    }

    public boolean isPresent() {
        for (String key : keysToTry.values()) {
            if (isPresentCertain(key)) return true;
        }

        return false;
    }

    public static boolean isPresentCertain(String toTry) {
        return ModuleUtils.serverHasPlugin(toTry);
    }
}
