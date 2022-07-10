package net.streamline.api.placeholder;

import net.streamline.api.savables.users.SavableUser;
import net.streamline.utils.MessagingUtils;

import java.util.ArrayList;
import java.util.List;

public class RATAPI {
    public RATAPI api;
    public List<RATExpansion> loadedExpansions = new ArrayList<>();

    public RATAPI() {
        this.api = this;
        MessagingUtils.logInfo("Replace A Thing (RAT) API Loaded... (A Placeholder API for Proxies.)");
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
