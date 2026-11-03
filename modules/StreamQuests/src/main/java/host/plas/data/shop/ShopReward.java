package host.plas.data.shop;

import host.plas.adapters.spigot.SpigotAdapter;
import host.plas.data.QuestManager;
import host.plas.utils.Numbers;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One thing a {@link ShopEntry} hands out. Items, experience and effects go through the server's
 * own {@code give}, {@code experience} and {@code effect} commands, run as the console with the
 * buyer's UUID as the target, so they work the same on Bukkit and the mod loaders and accept any
 * registered id, modded ones included.
 */
@Getter @Setter
public class ShopReward {
    private final ShopRewardType type;

    /** COMMAND: commands to run; MESSAGE / BROADCAST: lines to send. */
    private List<String> lines = new ArrayList<>();

    /** ITEM: namespaced item id. */
    private String item;
    /** ITEM: count; EXP: levels or points. */
    private int amount = 1;
    /** ITEM: components ({@code [...]}, 1.20.5+) or NBT ({@code {...}}, older) written right after the id. */
    private String data = "";

    /** EXP: whether {@link #amount} counts levels rather than points. */
    private boolean levels = true;

    /** POTION: namespaced effect id. */
    private String effect;
    /** POTION: seconds; zero or less is infinite. */
    private int duration = 60;
    /** POTION: effect level, 1 for level I. */
    private int level = 1;
    /** POTION: hides the effect's particles. */
    private boolean hideParticles = false;

    /** Overrides the line the shop shows for this reward. */
    private String display;

    public ShopReward(ShopRewardType type) {
        this.type = type;
    }

    /**
     * Whether the server has what this reward gives. Asked before taking points, since the
     * commands report nothing back and a missing id would cost the buyer for nothing. Cores
     * without the check count everything as present.
     */
    public boolean isAvailable() {
        try {
            switch (type) {
                case ITEM:
                    return item != null && Singularity.gameplay().map(h -> h.hasItemType(item)).orElse(true);
                case POTION:
                    return effect != null && Singularity.gameplay().map(h -> h.hasEffectType(effect)).orElse(true);
                default:
                    return true;
            }
        } catch (LinkageError olderCore) {
            return true;
        }
    }

    /** The lore line describing this reward, or {@code null} for ones that are not shown. */
    public String describe() {
        if (display != null) return display.isEmpty() ? null : display;
        switch (type) {
            case ITEM:
                return "&8- &f" + amount + "x &b" + Numbers.title(item);
            case EXP:
                return "&8- &a" + Numbers.format(amount) + (levels ? " &7XP levels" : " &7XP points");
            case POTION:
                return "&8- &d" + Numbers.title(effect) + " " + Numbers.roman(level)
                        + " &7(" + (duration <= 0 ? "infinite" : Numbers.duration(duration)) + ")";
            default:
                return null;
        }
    }

    public void give(CosmicPlayer buyer) {
        String target = buyer.getUuid();
        String vanilla = SpigotAdapter.isBukkit() ? "minecraft:" : "";

        switch (type) {
            case COMMAND:
                for (String command : lines) {
                    String line = tokens(buyer, command).trim();
                    if (line.isEmpty()) continue;
                    if (! line.startsWith("@")) line = "@c " + line;
                    QuestManager.runCommand(buyer, line);
                }
                break;
            case ITEM:
                console(vanilla + "give " + target + " " + item.trim() + (data == null ? "" : data.trim()) + " " + Math.max(1, amount));
                break;
            case EXP:
                console(vanilla + "experience add " + target + " " + amount + (levels ? " levels" : " points"));
                break;
            case POTION:
                console(vanilla + "effect give " + target + " " + effect.trim() + " "
                        + (duration <= 0 ? "infinite" : String.valueOf(duration)) + " "
                        + Math.max(0, level - 1) + " " + hideParticles);
                break;
            case MESSAGE:
                for (String line : lines) ModuleUtils.sendMessage(buyer, text(buyer, line));
                break;
            case BROADCAST:
                for (String line : lines) {
                    String message = text(buyer, line);
                    ModuleUtils.getOnlinePlayers().values().forEach(player -> ModuleUtils.sendMessage(player, message));
                    ModuleUtils.sendMessage(UserUtils.getConsole(), message);
                }
                break;
        }
    }

    private static void console(String command) {
        ModuleUtils.getConsole().runCommand(command);
    }

    private static String text(CosmicSender buyer, String line) {
        return ModuleUtils.replacePlaceholders(buyer, tokens(buyer, line));
    }

    /** {@code %player%} and {@code %uuid%}, the shop's shorthand for the buyer. */
    private static String tokens(CosmicSender buyer, String line) {
        if (line == null) return "";
        return line.replace("%player%", buyer.getCurrentName()).replace("%uuid%", buyer.getUuid());
    }

    @Override
    public String toString() {
        return type.name().toLowerCase(Locale.ROOT);
    }
}
