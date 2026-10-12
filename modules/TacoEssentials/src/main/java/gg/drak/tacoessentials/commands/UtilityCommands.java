package gg.drak.tacoessentials.commands;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.data.TacoDatabase;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.IGameplayHandler;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/** {@code /kickme}, {@code /trashcan}, {@code /hat}, {@code /nick}, {@code /ec}, plus nickname handling shared with {@code /nickfor}. */
public final class UtilityCommands {

    private static final int TRASH_ROWS = 4;
    private static final Pattern COLOR_CODE = Pattern.compile("(?i)&[0-9a-fk-orx]");

    private UtilityCommands() {}

    public static List<TacoCommand> create() {
        List<TacoCommand> commands = new ArrayList<>();
        commands.add(new TacoCommand("kickme", ctx -> ModuleUtils.kick(ctx.player(), "You kicked yourself. See you soon!")));
        commands.add(new TacoCommand("trashcan", ctx ->
                gameplay().openDisposal(ctx.player().getUuid(), "&8Trash Can - items are deleted on close", TRASH_ROWS)));
        commands.add(new TacoCommand("hat", UtilityCommands::hat));
        commands.add(new TacoCommand("nick", UtilityCommands::nick, UtilityCommands::nickTargets));
        commands.add(new TacoCommand("ec", UtilityCommands::enderChest,
                (ctx, arg) -> arg == 0 && ctx.has(Perms.EC_OTHERS) ? TacoCommand.onlineNames() : Collections.emptyList()));
        return commands;
    }

    /** {@code /ec (player)}: the runner's own ender chest, or {@code (player)}'s with the others permission. */
    private static void enderChest(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer viewer = ctx.player();
        CosmicPlayer owner = viewer;
        if (ctx.arg(0) != null) {
            if (! ctx.has(Perms.EC_OTHERS)) throw Msg.fail("You may only open your own ender chest.");
            owner = TacoCommand.onlinePlayer(ctx.arg(0));
        }
        if (! gameplay().openEnderChest(viewer.getUuid(), owner.getUuid())) {
            throw Msg.fail(owner.getCurrentName() + " is not online here.");
        }
    }

    private static void hat(TacoCommand.Ctx ctx) throws Msg.Fail {
        IGameplayHandler.HatResult result = gameplay().wearHeldItem(ctx.player().getUuid());
        switch (result) {
            case SUCCESS:
                ctx.reply(Msg.success("Enjoy your new hat!"));
                return;
            case EMPTY_HAND:
                throw Msg.fail("Hold the item you want to wear in your main hand.");
            case CURSED_HELMET:
                throw Msg.fail("Your current helmet has Curse of Binding.");
            default:
                throw Msg.fail("You are not online here.");
        }
    }

    private static final String NICK_USAGE = "/nick (<nick> (player)|\"<nick words...>\" (player)|<nick words...> -p:<player>)";
    private static final String TARGET_FLAG = "-p:";

    /**
     * {@code /nick (<nick> (player)|"<nick words...>" (player)|<nick words...> -p:<player>)}.
     * With no nickname the nickname is cleared, so {@code /nick} clears the runner's and
     * {@code /nick -p:<player>} another player's. Naming another player needs
     * {@link Perms#NICK_OTHERS}; without it every word belongs to the nickname.
     *
     * <p>{@code /nick <word> <word>} is read as a nickname and a player only when the second word
     * is the name of a player who has joined; otherwise both words are the nickname. Quotes or
     * {@code -p:} make it unambiguous.</p>
     */
    private static void nick(TacoCommand.Ctx ctx) throws Msg.Fail {
        boolean others = ctx.has(Perms.NICK_OTHERS);
        String raw = ctx.rest(0);
        String nickname = null;
        String targetName = null;

        if (raw != null) {
            List<String> words = new ArrayList<>(Arrays.asList(raw.split(" ")));
            if (others) {
                for (int i = words.size() - 1; i >= 0; i--) {
                    String word = words.get(i);
                    if (! word.regionMatches(true, 0, TARGET_FLAG, 0, TARGET_FLAG.length())) continue;
                    if (targetName != null) throw Msg.fail("Name only one player. Usage: " + NICK_USAGE);
                    targetName = word.substring(TARGET_FLAG.length());
                    if (targetName.isEmpty()) throw Msg.fail("Put the player's name right after -p: Usage: " + NICK_USAGE);
                    words.remove(i);
                }
            }
            String joined = String.join(" ", words);

            if (joined.startsWith("\"")) {
                int close = joined.indexOf('"', 1);
                if (close < 0) throw Msg.fail("Close the quoted nickname with \". Usage: " + NICK_USAGE);
                nickname = joined.substring(1, close);
                String after = joined.substring(close + 1).trim();
                if (! after.isEmpty()) {
                    if (! others || targetName != null || after.contains(" ")) throw Msg.fail("Usage: " + NICK_USAGE);
                    targetName = after;
                }
            } else if (others && targetName == null && words.size() == 2 && isKnownName(words.get(1))) {
                nickname = words.get(0);
                targetName = words.get(1);
            } else {
                nickname = joined;
            }
            if (nickname.isEmpty()) nickname = null;
        }

        String uuid;
        String name;
        if (targetName == null) {
            CosmicPlayer player = ctx.player();
            uuid = player.getUuid();
            name = null;
        } else {
            String[] known = TacoCommand.knownPlayer(targetName);
            uuid = known[0];
            name = ctx.isPlayer() && ctx.sender().getUuid().equals(uuid) ? null : known[1];
        }

        String value = nickname == null ? null : validateNickname(nickname);
        applyNickname(uuid, value);

        if (name == null) {
            ctx.reply(value == null ? Msg.success("Your nickname was cleared.") : Msg.success("Your nickname is now ") + "&r" + value);
            return;
        }
        ctx.reply(value == null ? Msg.success("Cleared " + name + "'s nickname.") : Msg.success(name + "'s nickname is now ") + "&r" + value);
        UserUtils.getPlayer(uuid).filter(CosmicPlayer::isOnline).ifPresent(target -> target.sendMessage(value == null
                ? Msg.success("Your nickname was cleared.")
                : Msg.success("Your nickname is now ") + "&r" + value));
    }

    /** Whether the name is an online player's or one this module has seen join; never asks the network. */
    private static boolean isKnownName(String name) {
        if (TacoDatabase.playerByName(name).isPresent()) return true;
        for (String online : TacoCommand.onlineNames()) if (online.equalsIgnoreCase(name)) return true;
        return false;
    }

    /** Player names, and {@code -p:<name>}, for runners allowed to name another player. */
    private static List<String> nickTargets(TacoCommand.Ctx ctx, int arg) {
        if (arg == 0 || ! ctx.has(Perms.NICK_OTHERS)) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String online : TacoCommand.onlineNames()) {
            if (arg == 1) out.add(online);
            out.add(TARGET_FLAG + online);
        }
        return out;
    }

    /** Checks length (not counting color codes) and whether colors are allowed. */
    static String validateNickname(String raw) throws Msg.Fail {
        String visible = COLOR_CODE.matcher(raw).replaceAll("");
        if (! TacoEssentials.getConfig().nickAllowColors() && ! visible.equals(raw)) {
            throw Msg.fail("Nicknames may not use color codes.");
        }
        if (visible.isBlank()) throw Msg.fail("A nickname needs at least one visible character.");
        int max = TacoEssentials.getConfig().nickMaxLength();
        if (visible.length() > max) throw Msg.fail("Nicknames may be at most " + max + " characters long.");
        return raw;
    }

    /**
     * Stores the nickname in the player's Streamline metadata (so it applies on every
     * Streamline platform and at the next login) and shows it now where the platform can.
     *
     * @param value the nickname, or {@code null} to clear it
     */
    static void applyNickname(String uuid, String value) {
        UserUtils.getOrCreatePlayer(uuid).ifPresent(player -> {
            player.getMeta().setNickname(value == null ? "" : value);
            player.save();
        });
        gameplay().refreshDisplayName(uuid);
    }
}
