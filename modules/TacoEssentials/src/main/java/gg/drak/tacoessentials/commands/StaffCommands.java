package gg.drak.tacoessentials.commands;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.data.Loc;
import gg.drak.tacoessentials.data.PendingActions;
import gg.drak.tacoessentials.data.Sessions;
import gg.drak.tacoessentials.data.TacoDatabase;
import gg.drak.tacoessentials.teleport.Teleports;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.interfaces.ISingularityExtension;
import singularity.modules.ModuleUtils;
import singularity.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/**
 * Staff teleports ({@code /tpo}, {@code /tphere}, {@code /tppos}) and moderation tools
 * ({@code /freeze}, {@code /sudo}, {@code /broadcast}, {@code /near}, {@code /findplayer}).
 *
 * <p>Staff teleports skip the countdown and record {@code /back}. An offline player is found where
 * they logged out, and moving an offline player happens at their next join. The vanilla
 * {@code /tp} is left alone, since command blocks and datapacks rely on its selectors.</p>
 */
public final class StaffCommands {

    private StaffCommands() {}

    public static List<TacoCommand> create() {
        List<TacoCommand> commands = new ArrayList<>();
        commands.add(new TacoCommand("tpo", StaffCommands::tpo, StaffCommands::tpoCompletions));
        commands.add(new TacoCommand("tphere", StaffCommands::tphere, StaffCommands::knownFirst, "tpohere"));
        commands.add(new TacoCommand("tppos", StaffCommands::tppos, StaffCommands::tpposCompletions, "tpopos"));
        commands.add(new TacoCommand("freeze", StaffCommands::freeze, StaffCommands::knownFirst));
        commands.add(new TacoCommand("sudo", StaffCommands::sudo,
                (ctx, arg) -> arg == 0 ? TacoCommand.onlineNames() : arg == 1 ? Collections.singletonList("c:") : Collections.emptyList()));
        commands.add(new TacoCommand("broadcast", StaffCommands::broadcast, null, "bc", "bcast"));
        commands.add(new TacoCommand("near", StaffCommands::near));
        commands.add(new TacoCommand("findplayer", StaffCommands::findPlayer, StaffCommands::knownFirst, "fp", "locateplayer"));
        return commands;
    }

    private static List<String> knownFirst(TacoCommand.Ctx ctx, int arg) {
        return arg == 0 ? TacoCommand.knownNames() : Collections.emptyList();
    }

    // ---- teleports ----

    /** Where a player is: live when online, else where they logged out. */
    private static Loc locate(TacoCommand.Target player) throws Msg.Fail {
        Optional<CosmicLocation> live = gameplay().getLocation(player.uuid);
        if (live.isPresent()) return Loc.of(live.get());
        return TacoDatabase.player(player.uuid).map(TacoDatabase.PlayerRecord::getLastLocation)
                .orElseThrow(() -> Msg.fail("No location is recorded for " + player.name + "."));
    }

    /** Moves {@code mover} now, or at their next join while offline; tells the runner which. */
    private static void move(TacoCommand.Ctx ctx, TacoCommand.Target mover, Loc to, String where) throws Msg.Fail {
        if (mover.online == null) {
            PendingActions.teleport(mover.uuid, to);
            ctx.reply(Msg.success(mover.name + " is offline; they will be sent to " + where + " when they next join."));
            return;
        }
        if (! Teleports.teleport(mover.uuid, to, true)) {
            throw Msg.fail("That location is in a world that no longer exists (" + to.getWorld() + ").");
        }
        mover.online.sendMessage(Msg.success("Teleported to " + where + "."));
        if (! mover.isSelf(ctx)) ctx.reply(Msg.success("Teleported " + mover.name + " to " + where + "."));
    }

    /** {@code /tpo <target> (player)} or {@code /tpo <x> <y> <z> (yaw) (pitch)}. */
    private static void tpo(TacoCommand.Ctx ctx) throws Msg.Fail {
        if (ctx.count() >= 3 && isCoordinate(ctx.arg(0))) {
            tppos(ctx);
            return;
        }
        String usage = "/tpo (<target> (player)|<x> <y> <z> (yaw) (pitch))";
        if (ctx.count() < 1 || ctx.count() > 2) throw Msg.fail("Usage: " + usage);
        TacoCommand.Target destination = TacoCommand.target(ctx.arg(0));
        TacoCommand.Target mover = TacoCommand.targetOrSelf(ctx, 1, Perms.STAFF_OTHERS, true);
        if (destination.uuid.equals(mover.uuid)) throw Msg.fail("That player is already there.");
        String where = destination.name + (destination.online == null ? "'s logout spot" : "");
        move(ctx, mover, locate(destination), where);
    }

    /** {@code /tphere <player>}: brings the player to the runner, now or at their next join. */
    private static void tphere(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer me = ctx.player();
        TacoCommand.Target mover = TacoCommand.target(ctx.require(0, "/tphere <player>"));
        if (mover.isSelf(ctx)) throw Msg.fail("You are already here.");
        CosmicLocation here = gameplay().getLocation(me.getUuid()).orElseThrow(() -> Msg.fail("You are not online here."));
        move(ctx, mover, Loc.of(here), me.getCurrentName());
    }

    /**
     * {@code /tppos (player) <x> <y> <z> (yaw) (pitch)}, in the moved player's world; {@code ~}
     * values are relative to them, and whole-number X/Z land in the middle of the block.
     */
    private static void tppos(TacoCommand.Ctx ctx) throws Msg.Fail {
        String usage = "/tppos (player) <x> <y> <z> (yaw) (pitch)";
        int start = ctx.arg(0) != null && ! isCoordinate(ctx.arg(0)) ? 1 : 0;
        int numbers = ctx.count() - start;
        if (numbers < 3 || numbers > 5) throw Msg.fail("Usage: " + usage);
        TacoCommand.Target mover = start == 1 ? TacoCommand.targetOrSelf(ctx, 0, Perms.STAFF_OTHERS, true) : TacoCommand.self(ctx);
        Loc base = locate(mover);

        String rawX = ctx.arg(start);
        String rawZ = ctx.arg(start + 2);
        double x = PositionCommands.coordinate(rawX, base.getX(), usage);
        double y = PositionCommands.coordinate(ctx.arg(start + 1), base.getY(), usage);
        double z = PositionCommands.coordinate(rawZ, base.getZ(), usage);
        if (isWhole(rawX)) x += 0.5;
        if (isWhole(rawZ)) z += 0.5;
        float yaw = ctx.arg(start + 3) == null ? base.getYaw() : PositionCommands.yaw(PositionCommands.coordinate(ctx.arg(start + 3), base.getYaw(), usage));
        float pitch = ctx.arg(start + 4) == null ? base.getPitch() : PositionCommands.pitch(PositionCommands.coordinate(ctx.arg(start + 4), base.getPitch(), usage));

        Loc to = new Loc(base.getWorld(), x, y, z, yaw, pitch);
        move(ctx, mover, to, String.format("%.1f, %.1f, %.1f", x, y, z));
    }

    private static boolean isWhole(String raw) {
        return ! raw.startsWith("~") && ! raw.contains(".");
    }

    static boolean isCoordinate(String token) {
        if (token == null || token.isEmpty()) return false;
        String number = token.startsWith("~") ? token.substring(1) : token;
        if (number.isEmpty()) return true;
        try {
            double value = Double.parseDouble(number);
            return ! Double.isNaN(value) && ! Double.isInfinite(value);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static List<String> tpoCompletions(TacoCommand.Ctx ctx, int arg) {
        if (arg == 0) return TacoCommand.knownNames();
        if (arg == 1 && ! isCoordinate(ctx.arg(0)) && ctx.has(Perms.STAFF_OTHERS)) return TacoCommand.knownNames();
        return Collections.emptyList();
    }

    private static List<String> tpposCompletions(TacoCommand.Ctx ctx, int arg) {
        List<String> out = new ArrayList<>();
        boolean named = ctx.arg(0) != null && ! isCoordinate(ctx.arg(0)) && arg > 0;
        if (arg == 0 && ctx.has(Perms.STAFF_OTHERS)) out.addAll(TacoCommand.onlineNames());
        int index = arg - (named ? 1 : 0);
        if (index <= 4) out.add("~");
        if (ctx.isPlayer() && index <= 2) {
            gameplay().getLocation(ctx.sender().getUuid()).ifPresent(here -> {
                double value = index == 0 ? here.getX() : index == 1 ? here.getY() : here.getZ();
                out.add(String.valueOf((int) Math.floor(value)));
            });
        }
        return out;
    }

    // ---- moderation ----

    /** {@code /freeze <player>}: toggles; saved, so an offline player is frozen when they join. */
    private static void freeze(TacoCommand.Ctx ctx) throws Msg.Fail {
        TacoCommand.Target target = TacoCommand.target(ctx.require(0, "/freeze <player>"));
        if (target.isSelf(ctx)) throw Msg.fail("You cannot freeze yourself.");
        boolean frozen = ! TacoDatabase.hasFlag(target.uuid, TacoDatabase.FLAG_FROZEN);
        Loc at = gameplay().getLocation(target.uuid).map(Loc::of).orElse(null);
        Sessions.setFrozen(target.uuid, frozen, at);
        if (target.online != null) {
            target.online.sendMessage(frozen ? Msg.error("You have been frozen by staff. You can still use /msg.")
                    : Msg.success("You are no longer frozen."));
        }
        ctx.reply(Msg.success((frozen ? "Froze " : "Unfroze ") + target.name + ".")
                + (target.online == null ? Msg.muted(" They are offline; it applies when they next join.") : ""));
    }

    /** {@code /sudo <player> <command...>} or {@code /sudo <player> c:<message>}. */
    private static void sudo(TacoCommand.Ctx ctx) throws Msg.Fail {
        String usage = "/sudo <player> (<command...>|c:<message>)";
        CosmicPlayer target = TacoCommand.onlinePlayer(ctx.require(0, usage));
        ctx.require(1, usage);
        String rest = ctx.rest(1);
        if (rest.regionMatches(true, 0, "c:", 0, 2)) {
            String message = rest.substring(2).trim();
            if (message.isEmpty()) throw Msg.fail("Usage: " + usage);
            // The mod loaders' chatAs runs its text as a command, so chatting as someone works on Spigot only.
            if (Singularity.getInstance().getPlatform().getPlatformType() != ISingularityExtension.PlatformType.SPIGOT) {
                throw Msg.fail("Chatting as another player is only supported on Spigot and Paper.");
            }
            target.asReal().chatAs(message);
            ctx.reply(Msg.success("Made " + target.getCurrentName() + " say: ") + "&f" + message);
            return;
        }
        String command = rest.startsWith("/") ? rest.substring(1) : rest;
        if (! ModuleUtils.runAs(target, command)) throw Msg.fail("Could not run that command as " + target.getCurrentName() + ".");
        ctx.reply(Msg.success("Made " + target.getCurrentName() + " run: ") + "&f/" + command);
    }

    /** {@code /broadcast <message>}, formatted by {@code broadcast.format}. */
    private static void broadcast(TacoCommand.Ctx ctx) throws Msg.Fail {
        ctx.require(0, "/broadcast <message>");
        String sender = ctx.isPlayer() ? ((CosmicPlayer) ctx.sender()).getCurrentName() : "Console";
        String line = TacoEssentials.getConfig().broadcastFormat()
                .replace("%sender%", sender)
                .replace("%message%", ctx.rest(0));
        Singularity.getInstance().getPlatform().getOnlinePlayers().forEach(p -> p.sendMessage(line));
        MessageUtils.logInfo("[Broadcast] " + ModuleUtils.stripColor(line));
    }

    /** {@code /near (distance)}: other players in the runner's world within (distance) blocks, nearest first. */
    private static void near(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer me = ctx.player();
        int max = TacoEssentials.getConfig().nearMaxRadius();
        int radius = ctx.arg(0) == null ? TacoEssentials.getConfig().nearDefaultRadius() : ctx.requireInt(0, "/near (distance)");
        if (radius < 1 || radius > max) throw Msg.fail("(distance) must be between 1 and " + max + ".");
        CosmicLocation here = gameplay().getLocation(me.getUuid()).orElseThrow(() -> Msg.fail("You are not online here."));

        List<double[]> found = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (CosmicPlayer other : Singularity.getInstance().getPlatform().getOnlinePlayers()) {
            if (other.getUuid().equals(me.getUuid())) continue;
            Optional<CosmicLocation> there = gameplay().getLocation(other.getUuid());
            if (there.isEmpty() || ! there.get().getWorldName().equals(here.getWorldName())) continue;
            double dx = there.get().getX() - here.getX();
            double dy = there.get().getY() - here.getY();
            double dz = there.get().getZ() - here.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > radius) continue;
            found.add(new double[] {distance, names.size()});
            names.add(other.getCurrentName());
        }
        if (found.isEmpty()) {
            ctx.reply(Msg.info("No players within " + radius + " blocks."));
            return;
        }
        found.sort((a, b) -> Double.compare(a[0], b[0]));
        List<String> parts = new ArrayList<>();
        for (double[] entry : found) parts.add("&f" + names.get((int) entry[1]) + Msg.muted(" (" + Math.round(entry[0]) + "m)"));
        ctx.reply(Msg.info("Players within " + radius + " blocks: ") + String.join(Msg.muted(", "), parts));
    }

    /** {@code /findplayer <player>}: where they are, or where they logged out. */
    private static void findPlayer(TacoCommand.Ctx ctx) throws Msg.Fail {
        TacoCommand.Target target = TacoCommand.target(ctx.require(0, "/findplayer <player>"));
        Optional<CosmicLocation> live = gameplay().getLocation(target.uuid);
        if (live.isPresent()) {
            ctx.reply(Msg.info(target.name + " is at ") + "&f" + Loc.of(live.get()).describe() + Msg.info("."));
            return;
        }
        Loc last = TacoDatabase.player(target.uuid).map(TacoDatabase.PlayerRecord::getLastLocation)
                .orElseThrow(() -> Msg.fail(target.name + " is offline and no logout location is recorded."));
        ctx.reply(Msg.info(target.name + " is offline; they logged out at ") + "&f" + last.describe() + Msg.info("."));
    }
}
