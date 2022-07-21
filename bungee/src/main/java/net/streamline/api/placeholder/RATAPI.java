package net.streamline.api.placeholder;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.utils.MessagingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class RATAPI {
    public RATAPI api;
    public List<RATExpansion> loadedExpansions = new ArrayList<>();
    @Getter @Setter
    private TreeMap<String, String> customPlaceholders = new TreeMap<>();

    public RATAPI() {
        this.api = this;
        MessagingUtils.logInfo("Replace A Thing (RAT) API Loaded... (A Placeholder API for Proxies.)");
    }

    public void addCustomPlaceholder(String key, String value) {
        getCustomPlaceholders().put(key, value);
    }

    public void removeCustomPlaceholder(String key) {
        getCustomPlaceholders().remove(key);
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

    public String parseAllPlaceholders(SavableUser of, String from) {
        for (String search : getCustomPlaceholders().keySet()) {
            RATResult result = PlaceholderUtils.parseCustomPlaceholder(search, getCustomPlaceholders().get(search), from);
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
