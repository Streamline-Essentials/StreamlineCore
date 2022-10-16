package net.streamline.api.placeholder;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.objects.AtomicString;
import net.streamline.api.objects.DatedNumber;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MathUtils;
import net.streamline.api.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RATAPI {
    @Getter
    private final RATAPI api;
    @Getter @Setter
    private ConcurrentSkipListMap<DatedNumber<Integer>, RATExpansion> loadedExpansions = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private ConcurrentSkipListMap<DatedNumber<Integer>, CustomPlaceholder> customPlaceholders = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private ConcurrentSkipListMap<StreamlineModule, List<CustomPlaceholder>> modularizedPlaceholders = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private AtomicInteger totalParsedPlaceholders = new AtomicInteger(0);

    public RATAPI() {
        this.api = this;
        setLoadedExpansions(new ConcurrentSkipListMap<>());
        setCustomPlaceholders(new ConcurrentSkipListMap<>());
        setModularizedPlaceholders(new ConcurrentSkipListMap<>());
        setTotalParsedPlaceholders(new AtomicInteger(0));
        MessageUtils.logInfo("Replace A Thing (RAT) API Loaded... (A Placeholder API for Proxies.)");
    }

    public void registerCustomPlaceholder(CustomPlaceholder placeholder) {
        getCustomPlaceholders().put(new DatedNumber<>(getCustomPlaceholders().size() + 1), placeholder);
    }

    public void unregisterCustomPlaceholder(CustomPlaceholder placeholder) {
        MathUtils.remove(getCustomPlaceholders(), placeholder);
    }

    public void registerModularizedPlaceholder(ModularizedPlaceholder placeholder) {
        registerCustomPlaceholder(placeholder);
        List<CustomPlaceholder> placeholders = getModularizedPlaceholders().get(placeholder.getModule());
        if (placeholders == null) placeholders = new ArrayList<>();
        placeholders.add(placeholder);
        getModularizedPlaceholders().put(placeholder.getModule(), placeholders);
    }

    public void unregisterModularizedPlaceholder(ModularizedPlaceholder placeholder) {
        unregisterCustomPlaceholder(placeholder);
        List<CustomPlaceholder> placeholders = getModularizedPlaceholders().get(placeholder.getModule());
        if (placeholders == null) placeholders = new ArrayList<>();
        placeholders.remove(placeholder);
        getModularizedPlaceholders().put(placeholder.getModule(), placeholders);
    }

    public void registerExpansion(RATExpansion expansion) {
        getLoadedExpansions().put(new DatedNumber<>(getLoadedExpansions().size() + 1), expansion);
        expansion.setEnabled(true);
    }

    public void unregisterExpansion(RATExpansion expansion) {
        MathUtils.remove(getLoadedExpansions(), expansion);
        expansion.setEnabled(false);
    }

    public void enableExpansion(RATExpansion expansion) {
        expansion.setEnabled(true);
    }

    public void disableExpansion(RATExpansion expansion) {
        expansion.setEnabled(false);
    }

    public RATExpansion getExpansionByIdentifier(String identifier) {
        for (RATExpansion expansion : getLoadedExpansions().values()) {
            if (expansion.getIdentifier().equals(identifier)) return expansion;
        }

        return null;
    }

    public CompletableFuture<String> parseAllPlaceholders(StreamlineUser of, String from) {
        if (from.contains(GivenConfigs.getMainConfig().placeholderCacheReleaseInput())) {
            getLoadedExpansions().forEach((integerDatedNumber, expansion) -> expansion.release());
            from = from.replace(GivenConfigs.getMainConfig().placeholderCacheReleaseInput(), GivenConfigs.getMainConfig().placeholderCacheReleaseOutput());
        }

        String finalFrom = from;
        return CompletableFuture.supplyAsync(() -> {
            AtomicString atomicString = new AtomicString(finalFrom);
            AtomicInteger integer = new AtomicInteger(0);
            PlaceholderUtils.getAllPlaceholderResults(of, finalFrom).forEach(result -> integer.getAndAdd(result.parse(atomicString)));

            if (integer.get() > 0) atomicString.set(parseAllPlaceholders(of, atomicString.get()).join());

            return atomicString.get();
        });
    }

    public CompletableFuture<String> parseAllLogicalPlaceholders(String from) {
        if (from.contains(GivenConfigs.getMainConfig().placeholderCacheReleaseInput())) {
            getLoadedExpansions().forEach((integerDatedNumber, expansion) -> expansion.release());
            from = from.replace(GivenConfigs.getMainConfig().placeholderCacheReleaseInput(), GivenConfigs.getMainConfig().placeholderCacheReleaseOutput());
        }

        String finalFrom = from;
        return CompletableFuture.supplyAsync(() -> {
            AtomicString atomicString = new AtomicString(finalFrom);
            AtomicInteger integer = new AtomicInteger(0);
//            for (RATResult result : PlaceholderUtils.getAllLogicalPlaceholderResults(from)) {
//                integer.getAndAdd(result.parse(atomicString));
//            }
            PlaceholderUtils.getAllLogicalPlaceholderResults(finalFrom).forEach(result -> integer.getAndAdd(result.parse(atomicString)));

            if (integer.get() > 0) atomicString.set(parseAllLogicalPlaceholders(atomicString.get()).join());

            return atomicString.get();
        });
    }

    public boolean isRegistered(RATExpansion expansion) {
        return getLoadedExpansions().containsValue(expansion);
    }
}
