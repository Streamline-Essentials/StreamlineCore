package host.plas.data.shop;

import host.plas.StreamQuests;
import host.plas.configs.ShopConfig;
import host.plas.data.players.QuestPlayer;
import host.plas.utils.Numbers;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The loaded quest shop and the purchase flow. */
public final class ShopManager {
    private ShopManager() {
    }

    public enum Result {
        PURCHASED,
        NOT_ENOUGH_POINTS,
        NO_PERMISSION,
        UNAVAILABLE,
        NOT_LOADED,
    }

    public static List<ShopCategory> getCategories() {
        return ShopConfig.getCategories();
    }

    public static List<ShopEntry> getEntries() {
        List<ShopEntry> entries = new ArrayList<>();
        for (ShopCategory category : getCategories()) entries.addAll(category.getEntries());
        return entries;
    }

    public static Optional<ShopEntry> getEntry(String identifier) {
        if (identifier == null) return Optional.empty();
        for (ShopEntry entry : getEntries()) {
            if (entry.getIdentifier().equalsIgnoreCase(identifier)) return Optional.of(entry);
        }
        return Optional.empty();
    }

    public static boolean canSee(CosmicPlayer viewer, ShopEntry entry) {
        if (entry.hasPermission() && ! ModuleUtils.hasPermission(viewer, entry.getPermission())) return false;
        return ! ShopConfig.isHideUnavailable() || entry.isAvailable();
    }

    /** The entries of a category this viewer may see. */
    public static List<ShopEntry> visibleEntries(CosmicPlayer viewer, ShopCategory category) {
        List<ShopEntry> visible = new ArrayList<>();
        for (ShopEntry entry : category.getEntries()) if (canSee(viewer, entry)) visible.add(entry);
        return visible;
    }

    /**
     * Takes the entry's price from the buyer's quest points and hands out its rewards, then tells
     * the buyer how it went. The check and the deduction hold the progress object's lock, so fast
     * repeated clicks cannot spend the same points twice.
     */
    public static Result purchase(CosmicPlayer buyer, QuestPlayer progress, ShopEntry entry) {
        Result result = take(buyer, progress, entry);
        String message;
        switch (result) {
            case PURCHASED: message = ShopConfig.getMessagePurchased(); break;
            case NOT_ENOUGH_POINTS: message = ShopConfig.getMessageNotEnoughPoints(); break;
            case NO_PERMISSION: message = ShopConfig.getMessageNoPermission(); break;
            case UNAVAILABLE: message = ShopConfig.getMessageUnavailable(); break;
            default: message = ShopConfig.getMessageNotLoaded(); break;
        }
        ModuleUtils.sendMessage(buyer, fill(message, entry, progress));
        return result;
    }

    private static Result take(CosmicPlayer buyer, QuestPlayer progress, ShopEntry entry) {
        if (progress == null || ! progress.isFullyLoaded()) return Result.NOT_LOADED;
        if (entry.hasPermission() && ! ModuleUtils.hasPermission(buyer, entry.getPermission())) return Result.NO_PERMISSION;
        if (! entry.isAvailable()) return Result.UNAVAILABLE;

        synchronized (progress) {
            if (progress.getPoints() < entry.getPrice()) return Result.NOT_ENOUGH_POINTS;
            progress.removePoints(entry.getPrice());
        }
        progress.save();

        for (ShopReward reward : entry.getRewards()) {
            try {
                reward.give(buyer);
            } catch (RuntimeException e) {
                StreamQuests.getInstance().logWarning("Quest shop reward " + reward + " of '" + entry.getIdentifier()
                        + "' failed for " + buyer.getCurrentName() + ": " + e);
            }
        }
        return Result.PURCHASED;
    }

    public static String fill(String message, ShopEntry entry, QuestPlayer progress) {
        if (message == null) return "";
        return message.replace("%item%", entry.getName())
                .replace("%price%", Numbers.format(entry.getPrice()))
                .replace("%points%", progress == null ? "0" : Numbers.format(progress.getPoints()))
                .replace("%missing%", progress == null ? Numbers.format(entry.getPrice())
                        : Numbers.format(Math.max(0, entry.getPrice() - progress.getPoints())));
    }
}
