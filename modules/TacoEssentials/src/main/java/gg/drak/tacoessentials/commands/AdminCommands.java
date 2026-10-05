package gg.drak.tacoessentials.commands;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.data.Loc;
import gg.drak.tacoessentials.data.Sessions;
import gg.drak.tacoessentials.data.TacoDatabase;
import gg.drak.tacoessentials.teleport.Teleports;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/** Operator commands: {@code /teleport_last}, {@code /tpx}, {@code /jump}, {@code /heal}, {@code /fly}, {@code /god}, {@code /invsee}, {@code /nickfor}, {@code /mute}, {@code /unmute}. */
public final class AdminCommands {

    private AdminCommands() {}

    public static List<TacoCommand> create() {
        List<TacoCommand> commands = new ArrayList<>();
        commands.add(new TacoCommand("teleport_last", AdminCommands::teleportLast, AdminCommands::knownFirst));
        commands.add(new TacoCommand("tpx", AdminCommands::tpx,
                (ctx, arg) -> arg == 0 ? gameplay().getWorldNames() : Collections.emptyList()));
        commands.add(new TacoCommand("jump", AdminCommands::jump));
        commands.add(new TacoCommand("heal", AdminCommands::heal, AdminCommands::onlineFirst));
        commands.add(new TacoCommand("fly", AdminCommands::fly, AdminCommands::onlineFirst));
        commands.add(new TacoCommand("god", AdminCommands::god, AdminCommands::onlineFirst));
        commands.add(new TacoCommand("invsee", AdminCommands::invsee, AdminCommands::onlineFirst));
        commands.add(new TacoCommand("nickfor", AdminCommands::nickFor, AdminCommands::knownFirst));
        commands.add(new TacoCommand("mute", ctx -> mute(ctx, true), AdminCommands::knownFirst));
        commands.add(new TacoCommand("unmute", ctx -> mute(ctx, false), AdminCommands::knownFirst));
        return commands;
    }

    private static List<String> onlineFirst(TacoCommand.Ctx ctx, int arg) {
        return arg == 0 ? TacoCommand.onlineNames() : Collections.emptyList();
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
        String world = resolveWorld(ctx.require(0, "/tpx <world>"));
        CosmicLocation here = gameplay().getLocation(player.getUuid()).orElseThrow(() -> Msg.fail("You are not online here."));
        int x = (int) Math.floor(here.getX());
        int z = (int) Math.floor(here.getZ());
        CosmicLocation spot = gameplay().findSafeLocation(world, x, z, (int) Math.floor(here.getY()))
                .orElseThrow(() -> Msg.fail("There is no safe spot at your X/Z in that world, or it is outside the world border."));
        Loc to = new Loc(world, here.getX(), spot.getY(), here.getZ(), here.getYaw(), here.getPitch());
        Teleports.teleport(player.getUuid(), to, true);
        ctx.reply(Msg.success("Teleported to " + Loc.shortWorld(world) + " at " + x + ", " + (int) spot.getY() + ", " + z + "."));
    }

    /** Accepts a world's full name or, for namespaced dimension ids, the part after the colon. */
    private static String resolveWorld(String name) throws Msg.Fail {
        List<String> worlds = gameplay().getWorldNames();
        for (String world : worlds) if (world.equalsIgnoreCase(name)) return world;
        for (String world : worlds) if (Loc.shortWorld(world).equalsIgnoreCase(name)) return world;
        throw Msg.fail("No world named " + name + ". Worlds: " + String.join(", ", worlds));
    }

    /** Teleports onto the first open, safe spot at or above the block being looked at. */
    private static void jump(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        int range = TacoEssentials.getConfig().jumpMaxDistance();
        CosmicLocation spot = gameplay().findTargetedLocation(player.getUuid(), range)
                .orElseThrow(() -> Msg.fail("No block with room on top in sight (range " + range + ")."));
        Teleports.teleport(player.getUuid(), Loc.of(spot), true);
    }

    /** The online player named by the first argument, or the runner when there is none. */
    private static CosmicPlayer targetOrSelf(TacoCommand.Ctx ctx) throws Msg.Fail {
        return ctx.arg(0) != null ? TacoCommand.onlinePlayer(ctx.arg(0)) : ctx.player();
    }

    private static boolean isSelf(TacoCommand.Ctx ctx, CosmicPlayer target) {
        return ctx.isPlayer() && ctx.sender().getUuid().equals(target.getUuid());
    }

    private static void heal(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer target = targetOrSelf(ctx);
        if (! gameplay().heal(target.getUuid())) throw Msg.fail(target.getCurrentName() + " is not online here.");
        target.sendMessage(Msg.success("You have been healed."));
        if (! isSelf(ctx, target)) ctx.reply(Msg.success("Healed " + target.getCurrentName() + "."));
    }

    /** Toggles flight; it stays on across respawns, world changes and relogs until toggled off. */
    private static void fly(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer target = targetOrSelf(ctx);
        boolean enable = ! Sessions.get(target.getUuid()).isFly();
        if (! gameplay().setFlight(target.getUuid(), enable)) throw Msg.fail(target.getCurrentName() + " is not online here.");
        Sessions.setFly(target.getUuid(), enable);
        target.sendMessage(Msg.success("Flight " + (enable ? "enabled." : "disabled.")));
        if (! isSelf(ctx, target)) {
            ctx.reply(Msg.success("Flight " + (enable ? "enabled" : "disabled") + " for " + target.getCurrentName() + "."));
        }
    }

    /** Toggles god mode; it stays on across respawns, world changes and relogs until toggled off. */
    private static void god(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer target = targetOrSelf(ctx);
        boolean enable = ! Sessions.get(target.getUuid()).isGod();
        if (! gameplay().setGodMode(target.getUuid(), enable)) throw Msg.fail(target.getCurrentName() + " is not online here.");
        Sessions.setGod(target.getUuid(), enable);
        target.sendMessage(Msg.success("God mode " + (enable ? "enabled." : "disabled.")));
        if (! isSelf(ctx, target)) {
            ctx.reply(Msg.success("God mode " + (enable ? "enabled" : "disabled") + " for " + target.getCurrentName() + "."));
        }
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
