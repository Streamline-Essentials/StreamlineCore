package net.streamline.api.placeholder;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.objects.DatedNumber;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class RATAPI {
    @Getter
    private final RATAPI api;
    @Getter @Setter
    private ConcurrentSkipListMap<DatedNumber<Integer>, RATExpansion> loadedExpansions = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private ConcurrentSkipListMap<DatedNumber<Integer>, CustomPlaceholder> customPlaceholders = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private ConcurrentHashMap<StreamlineModule, List<CustomPlaceholder>> modularizedPlaceholders = new ConcurrentHashMap<>();

    public RATAPI() {
        this.api = this;
        SLAPI.getInstance().getMessenger().logInfo("Replace A Thing (RAT) API Loaded... (A Placeholder API for Proxies.)");
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

    public String parseAllPlaceholders(StreamlineUser of, String from) {
        for (CustomPlaceholder placeholder : getCustomPlaceholders().values()) {
            RATResult result = PlaceholderUtils.parseCustomPlaceholder(placeholder.getKey(), placeholder.getValue(), from);
            if (result.didReplacement()) {
                from = result.string;
                from = parseAllPlaceholders(of, from);
            }
        }

        for (RATExpansion expansion : getLoadedExpansions().values()) {
            RATResult result = PlaceholderUtils.parsePlaceholder(expansion, of, from);
            if (result.didReplacement()) {
                from = result.string;
                from = parseAllPlaceholders(of, from);
            }
        }

        return from;
    }

    public boolean isRegistered(RATExpansion expansion) {
        return getLoadedExpansions().containsValue(expansion);
    }
}
