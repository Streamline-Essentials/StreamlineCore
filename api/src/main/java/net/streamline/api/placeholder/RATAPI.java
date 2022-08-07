package net.streamline.api.placeholder;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RATAPI {
    public RATAPI api;
    public List<RATExpansion> loadedExpansions = new ArrayList<>();
    @Getter @Setter
    private List<CustomPlaceholder> customPlaceholders = new ArrayList<>();
    @Getter @Setter
    private ConcurrentHashMap<StreamlineModule, List<CustomPlaceholder>> modularizedPlaceholders = new ConcurrentHashMap<>();

    public RATAPI() {
        this.api = this;
        SLAPI.getInstance().getMessenger().logInfo("Replace A Thing (RAT) API Loaded... (A Placeholder API for Proxies.)");
    }

    public void registerCustomPlaceholder(CustomPlaceholder placeholder) {
        getCustomPlaceholders().add(placeholder);
    }

    public void unregisterCustomPlaceholder(CustomPlaceholder placeholder) {
        getCustomPlaceholders().remove(placeholder);
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
        loadedExpansions.add(expansion.setEnabled(true));
    }

    public void unregisterExpansion(RATExpansion expansion) {
        loadedExpansions.remove(expansion.setEnabled(false));
    }

    public void enableExpansion(RATExpansion expansion) {
        expansion.setEnabled(true);
    }

    public void disableExpansion(RATExpansion expansion) {
        expansion.setEnabled(false);
    }

    public RATExpansion getExpansionByIdentifier(String identifier) {
        for (RATExpansion expansion : loadedExpansions) {
            if (expansion.identifier.equals(identifier)) return expansion;
        }

        return null;
    }

    public String parseAllPlaceholders(StreamlineUser of, String from) {
        for (CustomPlaceholder placeholder : getCustomPlaceholders()) {
            RATResult result = PlaceholderUtils.parseCustomPlaceholder(placeholder.getKey(), placeholder.getValue(), from);
            from = result.string;
            if (result.didReplacement()) {
                from = parseAllPlaceholders(of, from);
            }
        }

        for (RATExpansion expansion : loadedExpansions) {
            RATResult result = PlaceholderUtils.parsePlaceholder(expansion, of, from);
            from = result.string;
            if (result.didReplacement()) {
                from = parseAllPlaceholders(of, from);
            }
        }

        return from;
    }
}
