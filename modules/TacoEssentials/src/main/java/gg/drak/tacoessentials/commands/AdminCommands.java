package gg.drak.tacoessentials.commands;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.data.Loc;
import gg.drak.tacoessentials.data.PendingActions;
import gg.drak.tacoessentials.data.Sessions;
import gg.drak.tacoessentials.data.TacoDatabase;
import gg.drak.tacoessentials.teleport.Teleports;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/**
 * Operator commands: {@code /teleport_last}, {@code /tpx}, {@code /jump}, {@code /heal},
 * {@code /feed}, {@code /fly}, {@code /flyspeed}, {@code /walkspeed}, {@code /gm} (and
 * {@code /gms}, {@code /gmc}, {@code /gma}, {@code /gmsp}), {@code /god}, {@code /invsee},
 * {@code /nickfor}, {@code /mute}, {@code /unmute}.
 *
 * <p>The player tools act on the runner, or with {@link Perms#STAFF_OTHERS} on a named player.
 * A named player who is offline gets the change at their next join: heals, feeds and game mode
 * changes are queued, while flight and god mode are saved per player anyway.</p>
 */
public final class AdminCommands {

    private AdminCommands() {}

    public static List<TacoCommand> create() {
        List<TacoCommand> commands = new ArrayList<>();
        commands.add(new TacoCommand("teleport_last", AdminCommands::teleportLast, AdminCommands::knownFirst));
        commands.add(new TacoCommand("tpx", AdminCommands::tpx,
                (ctx, arg) -> arg == 0 ? gameplay().getWorldNames() : Collections.emptyList()));
        commands.add(new TacoCommand("jump", AdminCommands::jump));
        commands.add(new TacoCommand("heal", AdminCommands::heal, AdminCommands::othersFirst));
        commands.add(new TacoCommand("feed", AdminCommands::feed, AdminCommands::othersFirst));
        commands.add(new TacoCommand("fly", AdminCommands::fly, AdminCommands::othersFirst));
        commands.add(new TacoCommand("flyspeed", ctx -> speed(ctx, "flyspeed", "fly", 10F, gameplay()::setFlySpeed),
                speeds(FLY_SPEEDS), "fspeed"));
        commands.add(new TacoCommand("walkspeed", ctx -> speed(ctx, "walkspeed", "walk", 5F, gameplay()::setWalkSpeed),
                speeds(WALK_SPEEDS), "wspeed"));
        commands.add(new TacoCommand("gm", AdminCommands::gm,
                (ctx, arg) -> arg == 0 ? GAME_MODES : arg == 1 && ctx.has(Perms.STAFF_OTHERS) ? TacoCommand.onlineNames() : Collections.emptyList()));
        commands.add(new TacoCommand("gms", ctx -> setGameMode(ctx, "survival", 0), AdminCommands::othersFirst));
        commands.add(new TacoCommand("gmc", ctx -> setGameMode(ctx, "creative", 0), AdminCommands::othersFirst));
        commands.add(new TacoCommand("gma", ctx -> setGameMode(ctx, "adventure", 0), AdminCommands::othersFirst));
        commands.add(new TacoCommand("gmsp", ctx -> setGameMode(ctx, "spectator", 0), AdminCommands::othersFirst));
        commands.add(new TacoCommand("god", AdminCommands::god, AdminCommands::othersFirst));
        commands.add(new TacoCommand("invsee", AdminCommands::invsee, AdminCommands::onlineFirst, "inv", "cinv", "oi"));
        commands.add(new TacoCommand("nickfor", AdminCommands::nickFor, AdminCommands::knownFirst));
        commands.add(new TacoCommand("mute", ctx -> mute(ctx, true), AdminCommands::knownFirst));
        commands.add(new TacoCommand("unmute", ctx -> mute(ctx, false), AdminCommands::knownFirst));
        return commands;
    }

    private static List<String> onlineFirst(TacoCommand.Ctx ctx, int arg) {
        return arg == 0 ? TacoCommand.onlineNames() : Collections.emptyList();
    }

    private static List<String> othersFirst(TacoCommand.Ctx ctx, int arg) {
        return arg == 0 && ctx.has(Perms.STAFF_OTHERS) ? TacoCommand.onlineNames() : Collections.emptyList();
    }

    private static List<String> knownFirst(TacoCommand.Ctx ctx, int arg) {
        return arg == 0 ? TacoCommand.knownNames() : Collections.emptyList();
    }

    /** Online players: their current position. Offline players: where they logged out. */
    private static void teleportLast(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        String[] known = TacoCommand.knownPlayer(ctx.require(0, "/teleport_last <player>"));
        Optional<CosmicLocation> live = gameplay().getLocation(known[0]);
        Loc loc;
        String who;
        if (live.isPresent()) {
            loc = Loc.of(live.get());
            who = known[1] + " (online)";
        } else {
            Optional<TacoDatabase.PlayerRecord> record = TacoDatabase.player(known[0]);
            if (record.isEmpty() || record.get().getLastLocation() == null) {
                throw Msg.fail("No last location is recorded for " + known[1] + ".");
            }
            loc = record.get().getLastLocation();
            who = known[1];
        }
        if (! Teleports.teleport(player.getUuid(), loc, true)) {
            throw Msg.fail("That location is in a world that no longer exists (" + loc.getWorld() + ").");
        }
        ctx.reply(Msg.success("Teleported to the last location of " + who + ": " + loc.describe() + "."));
    }

    /** Same X/Z in another world (no Nether scaling), at the safe Y nearest the current one. */
    private static void tpx(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        String world = TacoCommand.resolveWorld(ctx.require(0, "/tpx <world>"));
        CosmicLocation here = gameplay().getLocation(player.getUuid()).orElseThrow(() -> Msg.fail("You are not online here."));
        int x = (int) Math.floor(here.getX());
        int z = (int) Math.floor(here.getZ());
        CosmicLocation spot = gameplay().findSafeLocation(world, x, z, (int) Math.floor(here.getY()))
                .orElseThrow(() -> Msg.fail("There is no safe spot at your X/Z in that world, or it is outside the world border."));
        Loc to = new Loc(world, here.getX(), spot.getY(), here.getZ(), here.getYaw(), here.getPitch());
        Teleports.teleport(player.getUuid(), to, true);
        ctx.reply(Msg.success("Teleported to " + Loc.shortWorld(world) + " at " + x + ", " + (int) spot.getY() + ", " + z + "."));
    }

    /** Teleports onto the first open, safe spot at or above the block being looked at. */
    private static void jump(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        int range = TacoEssentials.getConfig().jumpMaxDistance();
        CosmicLocation spot = gameplay().findTargetedLocation(player.getUuid(), range)
                .orElseThrow(() -> Msg.fail("No block with room on top in sight (range " + range + ")."));
        Teleports.teleport(player.getUuid(), Loc.of(spot), true);
    }

    /** Tells the runner and the target what was done; an offline target hears nothing until it applies. */
    private static void report(TacoCommand.Ctx ctx, TacoCommand.Target target, String toTarget, String toRunner) {
        if (target.online == null) {
            ctx.reply(Msg.success(toRunner) + Msg.muted(" " + target.name + " is offline; it applies when they next join."));
            return;
        }
        target.online.sendMessage(Msg.success(toTarget));
        if (! target.isSelf(ctx)) ctx.reply(Msg.success(toRunner));
    }

    private static void heal(TacoCommand.Ctx ctx) throws Msg.Fail {
        TacoCommand.Target target = TacoCommand.targetOrSelf(ctx, 0, Perms.STAFF_OTHERS, true);
        if (target.online == null) PendingActions.heal(target.uuid);
        else if (! gameplay().heal(target.uuid)) throw Msg.fail(target.name + " is not online here.");
        report(ctx, target, "You have been healed.", "Healed " + target.name + ".");
    }

    private static void feed(TacoCommand.Ctx ctx) throws Msg.Fail {
        TacoCommand.Target target = TacoCommand.targetOrSelf(ctx, 0, Perms.STAFF_OTHERS, true);
        if (target.online == null) PendingActions.feed(target.uuid);
        else if (! gameplay().feed(target.uuid)) throw Msg.fail(target.name + " is not online here.");
        report(ctx, target, "You have been fed.", "Fed " + target.name + ".");
    }

    /** Toggles flight; it stays on across respawns, world changes and relogs until toggled off. */
    private static void fly(TacoCommand.Ctx ctx) throws Msg.Fail {
        TacoCommand.Target target = TacoCommand.targetOrSelf(ctx, 0, Perms.STAFF_OTHERS, true);
        boolean enable;
        if (target.online == null) {
            enable = ! TacoDatabase.player(target.uuid).map(TacoDatabase.PlayerRecord::isFly).orElse(false);
        } else {
            enable = ! Sessions.get(target.uuid).isFly();
            if (! gameplay().setFlight(target.uuid, enable)) throw Msg.fail(target.name + " is not online here.");
        }
        Sessions.setFly(target.uuid, enable);
        String state = enable ? "enabled" : "disabled";
        report(ctx, target, "Flight " + state + ".", "Flight " + state + " for " + target.name + ".");
    }

    private static final List<String> FLY_SPEEDS = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "reset");
    private static final List<String> WALK_SPEEDS = List.of("1", "2", "3", "4", "5", "reset");

    /** Changes one player's fly or walk speed; {@link #speed} parses and checks the arguments for it. */
    private interface SpeedSetter {
        boolean set(String uuid, float multiplier);
    }

    private static TacoCommand.Completer speeds(List<String> speeds) {
        return (ctx, arg) -> arg == 0 ? speeds : arg == 1 && ctx.has(Perms.STAFF_OTHERS) ? TacoCommand.onlineNames() : Collections.emptyList();
    }

    /**
     * {@code /<label> <speed|reset> (player)}: speed is a multiple of the normal speed, from 0 to
     * {@code max}; {@code reset} sets it back to 1. The game saves speeds, so the player must be online.
     */
    private static void speed(TacoCommand.Ctx ctx, String label, String what, float max, SpeedSetter setter) throws Msg.Fail {
        String usage = "/" + label + " <speed|reset> (player)";
        String raw = ctx.require(0, usage);
        float speed;
        if (raw.equalsIgnoreCase("reset")) {
            speed = 1F;
        } else {
            try {
                speed = Float.parseFloat(raw);
            } catch (NumberFormatException e) {
                throw Msg.fail("'" + raw + "' is not a number. Usage: " + usage);
            }
            String shownMax = String.valueOf((int) max);
            if (! (speed >= 0F && speed <= max)) throw Msg.fail(capitalize(what) + " speed must be between 0 and " + shownMax + " (1 is normal).");
        }
        TacoCommand.Target target = TacoCommand.targetOrSelf(ctx, 1, Perms.STAFF_OTHERS, false);
        if (! setter.set(target.uuid, speed)) throw Msg.fail(target.name + " is not online here.");
        String shown = speed == (int) speed ? String.valueOf((int) speed) : String.valueOf(speed);
        report(ctx, target, capitalize(what) + " speed set to " + shown + ".", "Set " + target.name + "'s " + what + " speed to " + shown + ".");
    }

    private static String capitalize(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    private static final List<String> GAME_MODES = List.of("survival", "creative", "adventure", "spectator");

    /** A game mode by name, first letter(s) ({@code s}, {@code c}, {@code a}, {@code sp}) or number (0 to 3). */
    private static String gameMode(String raw) throws Msg.Fail {
        switch (raw.toLowerCase(Locale.ROOT)) {
            case "survival": case "s": case "0": return "survival";
            case "creative": case "c": case "1": return "creative";
            case "adventure": case "a": case "2": return "adventure";
            case "spectator": case "sp": case "3": return "spectator";
            default: throw Msg.fail("Unknown game mode '" + raw + "'. Use survival, creative, adventure or spectator.");
        }
    }

    /** {@code /gm <gamemode> (player)}. */
    private static void gm(TacoCommand.Ctx ctx) throws Msg.Fail {
        String mode = gameMode(ctx.require(0, "/gm <gamemode> (player)"));
        setGameMode(ctx, mode, 1);
    }

    /** Sets the game mode of the player named at {@code index}, or of the sender when there is none. */
    private static void setGameMode(TacoCommand.Ctx ctx, String mode, int index) throws Msg.Fail {
        TacoCommand.Target target = TacoCommand.targetOrSelf(ctx, index, Perms.STAFF_OTHERS, true);
        if (target.online == null) PendingActions.gameMode(target.uuid, mode);
        else if (! gameplay().setGameMode(target.uuid, mode)) throw Msg.fail(target.name + " is not online here.");
        String shown = capitalize(mode);
        report(ctx, target, "Game mode set to " + shown + ".", "Set " + target.name + "'s game mode to " + shown + ".");
    }

    /** Toggles god mode; it stays on across respawns, world changes and relogs until toggled off. */
    private static void god(TacoCommand.Ctx ctx) throws Msg.Fail {
        TacoCommand.Target target = TacoCommand.targetOrSelf(ctx, 0, Perms.STAFF_OTHERS, true);
        boolean enable;
        if (target.online == null) {
            enable = ! TacoDatabase.isGod(target.uuid);
        } else {
            enable = ! Sessions.get(target.uuid).isGod();
            if (! gameplay().setGodMode(target.uuid, enable)) throw Msg.fail(target.name + " is not online here.");
        }
        Sessions.setGod(target.uuid, enable);
        String state = enable ? "enabled" : "disabled";
        report(ctx, target, "God mode " + state + ".", "God mode " + state + " for " + target.name + ".");
    }

    private static void invsee(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer viewer = ctx.player();
        CosmicPlayer target = TacoCommand.onlinePlayer(ctx.require(0, "/invsee <player>"));
        if (target.getUuid().equals(viewer.getUuid())) throw Msg.fail("Open your own inventory with E.");
        if (! gameplay().openInventoryOf(viewer.getUuid(), target.getUuid())) {
            throw Msg.fail(target.getCurrentName() + " is not online here.");
        }
    }

    /** {@code /nickfor <player> (name…)}; with no name the nickname is cleared. Works for offline players. */
    private static void nickFor(TacoCommand.Ctx ctx) throws Msg.Fail {
        String[] known = TacoCommand.knownPlayer(ctx.require(0, "/nickfor <player> (name...)"));
        String raw = ctx.rest(1);
        if (raw == null) {
            UtilityCommands.applyNickname(known[0], null);
            ctx.reply(Msg.success("Cleared " + known[1] + "'s nickname."));
            return;
        }
        String value = UtilityCommands.validateNickname(raw);
        UtilityCommands.applyNickname(known[0], value);
        ctx.reply(Msg.success(known[1] + "'s nickname is now ") + "&r" + value);
    }

    private static void mute(TacoCommand.Ctx ctx, boolean muted) throws Msg.Fail {
        String[] known = TacoCommand.knownPlayer(ctx.require(0, muted ? "/mute <player>" : "/unmute <player>"));
        boolean current = TacoDatabase.player(known[0]).map(TacoDatabase.PlayerRecord::isMuted).orElse(false);
        if (current == muted) throw Msg.fail(known[1] + " is " + (muted ? "already" : "not") + " muted.");
        Sessions.setMuted(known[0], muted);
        UserUtils.getPlayer(known[0]).filter(CosmicPlayer::isOnline).ifPresent(online ->
                online.sendMessage(muted ? Msg.error("You have been muted.") : Msg.success("You are no longer muted.")));
        ctx.reply(Msg.success((muted ? "Muted " : "Unmuted ") + known[1] + "."));
    }
}
