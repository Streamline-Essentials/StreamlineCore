package gg.drak.tacoessentials.commands;

import gg.drak.tacoessentials.TacoEssentials;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.IGameplayHandler;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/** {@code /kickme}, {@code /trashcan}, {@code /hat}, {@code /nick}, plus nickname handling shared with {@code /nickfor}. */
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
        commands.add(new TacoCommand("nick", UtilityCommands::nick));
        return commands;
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

    /** {@code /nick (name…)}; with no name, clears the nickname. */
    private static void nick(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        String raw = ctx.rest(0);
        if (raw == null) {
            applyNickname(player.getUuid(), null);
            ctx.reply(Msg.success("Your nickname was cleared."));
            return;
        }
        String value = validateNickname(raw);
        applyNickname(player.getUuid(), value);
        ctx.reply(Msg.success("Your nickname is now ") + "&r" + value);
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
