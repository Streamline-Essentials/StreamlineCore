package gg.drak.tacoessentials.commands;

import gg.drak.tacoessentials.data.Loc;
import gg.drak.tacoessentials.teleport.Teleports;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/** {@code /up} ({@code /upper}), {@code /down}, {@code /top}, {@code /center} and {@code /rotate}. */
public final class PositionCommands {

    /** Furthest {@code /up} and {@code /down} search when given no distance: past any world's height. */
    private static final int WHOLE_COLUMN = 8192;

    private PositionCommands() {}

    public static List<TacoCommand> create() {
        List<TacoCommand> commands = new ArrayList<>();
        commands.add(new TacoCommand("up", ctx -> vertical(ctx, true), null, "upper"));
        commands.add(new TacoCommand("down", ctx -> vertical(ctx, false)));
        commands.add(new TacoCommand("top", PositionCommands::top));
        commands.add(new TacoCommand("center", PositionCommands::center));
        commands.add(new TacoCommand("rotate", PositionCommands::rotate, PositionCommands::rotateCompletions));
        return commands;
    }

    private static CosmicLocation here(CosmicPlayer player) throws Msg.Fail {
        return gameplay().getLocation(player.getUuid()).orElseThrow(() -> Msg.fail("You are not online here."));
    }

    /**
     * {@code /up (blocks)} and {@code /down (blocks)}: the nearest safe spot above or below the
     * player's feet in their column, at most {@code (blocks)} away, so a floor or ceiling in
     * between is passed through.
     */
    private static void vertical(TacoCommand.Ctx ctx, boolean up) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        String usage = up ? "/up (blocks)" : "/down (blocks)";
        int blocks = WHOLE_COLUMN;
        if (ctx.arg(0) != null) {
            blocks = ctx.requireInt(0, usage);
            if (blocks < 1) throw Msg.fail("(blocks) must be at least 1.");
            blocks = Math.min(blocks, WHOLE_COLUMN);
        }

        CosmicLocation here = here(player);
        int feet = (int) Math.floor(here.getY());
        int from = up ? feet + 1 : feet - 1;
        int to = up ? feet + blocks : feet - blocks;
        CosmicLocation spot = gameplay().findSafeLocationInColumn(here.getWorldName(),
                        (int) Math.floor(here.getX()), (int) Math.floor(here.getZ()), from, to)
                .orElseThrow(() -> Msg.fail("There is no safe spot " + (up ? "above" : "below") + " you"
                        + (ctx.arg(0) != null ? " within " + ctx.arg(0) + " blocks." : ".")));

        moveKeepingFacing(player, here, spot);
        int distance = Math.abs((int) spot.getY() - feet);
        ctx.reply(Msg.success("Moved " + (up ? "up " : "down ") + distance + " block" + (distance == 1 ? "" : "s") + "."));
    }

    /** {@code /top}: the highest safe spot in the player's column. */
    private static void top(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        CosmicLocation here = here(player);
        CosmicLocation spot = gameplay().findSafeLocationInColumn(here.getWorldName(),
                        (int) Math.floor(here.getX()), (int) Math.floor(here.getZ()), WHOLE_COLUMN, -WHOLE_COLUMN)
                .orElseThrow(() -> Msg.fail("There is no safe spot in this column."));
        moveKeepingFacing(player, here, spot);
        ctx.reply(Msg.success("Teleported to the top (y " + (int) spot.getY() + ")."));
    }

    /** {@code /center}: the middle of the block the player stands on, at their current height and facing. */
    private static void center(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        CosmicLocation here = here(player);
        Loc to = new Loc(here.getWorldName(), Math.floor(here.getX()) + 0.5, here.getY(), Math.floor(here.getZ()) + 0.5,
                here.getYaw(), here.getPitch());
        if (! Teleports.teleport(player.getUuid(), to, false)) throw Msg.fail("You are not online here.");
        ctx.reply(Msg.success("Centered on your block."));
    }

    /** {@code /rotate <pitch> (yaw)}: turns the player in place; {@code ~} values are relative. */
    private static void rotate(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        String usage = "/rotate <pitch> (yaw)";
        CosmicLocation here = here(player);
        float pitch = pitch(coordinate(ctx.require(0, usage), here.getPitch(), usage));
        float yaw = ctx.arg(1) == null ? here.getYaw() : yaw(coordinate(ctx.arg(1), here.getYaw(), usage));

        Loc to = new Loc(here.getWorldName(), here.getX(), here.getY(), here.getZ(), yaw, pitch);
        if (! Teleports.teleport(player.getUuid(), to, false)) throw Msg.fail("You are not online here.");
        ctx.reply(Msg.success(String.format("Now facing pitch %.1f, yaw %.1f.", pitch, yaw)));
    }

    /** Lands on {@code spot}'s block center, keeping the player's facing, and records {@code /back}. */
    private static void moveKeepingFacing(CosmicPlayer player, CosmicLocation here, CosmicLocation spot) throws Msg.Fail {
        Loc to = new Loc(spot.getWorldName(), spot.getX(), spot.getY(), spot.getZ(), here.getYaw(), here.getPitch());
        if (! Teleports.teleport(player.getUuid(), to, true)) throw Msg.fail("You are not online here.");
    }

    /** A number, or {@code ~} / {@code ~offset} relative to {@code current}. */
    static double coordinate(String raw, double current, String usage) throws Msg.Fail {
        boolean relative = raw.startsWith("~");
        String number = relative ? raw.substring(1) : raw;
        if (relative && number.isEmpty()) return current;
        try {
            double value = Double.parseDouble(number);
            if (Double.isNaN(value) || Double.isInfinite(value)) throw new NumberFormatException();
            return relative ? current + value : value;
        } catch (NumberFormatException e) {
            throw Msg.fail("'" + raw + "' is not a number. Usage: " + usage);
        }
    }

    /** Pitch runs from -90 (straight up) to 90 (straight down). */
    static float pitch(double value) {
        return (float) Math.max(-90.0, Math.min(90.0, value));
    }

    /** Yaw wrapped into [-180, 180). */
    static float yaw(double value) {
        double wrapped = ((value + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
        return (float) wrapped;
    }

    private static List<String> rotateCompletions(TacoCommand.Ctx ctx, int arg) {
        if (! ctx.isPlayer() || arg > 1) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        out.add("~");
        if (arg == 0) {
            out.add("0");
            out.add("-90");
            out.add("90");
        } else {
            out.add("0");
            out.add("90");
            out.add("180");
            out.add("-90");
        }
        return out;
    }
}
