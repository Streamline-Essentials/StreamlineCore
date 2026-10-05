package gg.drak.tacoessentials.commands;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.data.Loc;
import gg.drak.tacoessentials.data.TacoDatabase;
import gg.drak.tacoessentials.teleport.Teleports;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/** {@code /sethome}, {@code /home}, {@code /delhome}, {@code /listhomes}, {@code /warp}, {@code /listwarps}, {@code /setwarp}, {@code /delwarp}. */
public final class HomeCommands {

    /** The home {@code /home} picks when no name is given and the player has several. */
    static final String DEFAULT_HOME = "home";

    private HomeCommands() {}

    public static List<TacoCommand> create() {
        List<TacoCommand> commands = new ArrayList<>();
        commands.add(new TacoCommand("sethome", HomeCommands::setHome, HomeCommands::ownHomes));
        commands.add(new TacoCommand("home", HomeCommands::home, HomeCommands::ownHomes));
        commands.add(new TacoCommand("delhome", HomeCommands::delHome, HomeCommands::ownHomes));
        commands.add(new TacoCommand("listhomes", HomeCommands::listHomes,
                (ctx, arg) -> arg == 0 && ctx.has(Perms.LISTHOMES_OTHERS) ? TacoCommand.knownNames() : Collections.emptyList()));

        commands.add(new TacoCommand("warp", HomeCommands::warp, HomeCommands::warpNames));
        commands.add(new TacoCommand("listwarps", ctx -> listWarps(ctx)));
        commands.add(new TacoCommand("setwarp", HomeCommands::setWarp, HomeCommands::warpNames));
        commands.add(new TacoCommand("delwarp", HomeCommands::delWarp, HomeCommands::warpNames));
        return commands;
    }

    private static List<String> ownHomes(TacoCommand.Ctx ctx, int arg) {
        List<String> names = new ArrayList<>();
        if (arg != 0 || ! ctx.isPlayer()) return names;
        TacoDatabase.homes(ctx.sender().getUuid()).forEach(h -> names.add(h.getName()));
        return names;
    }

    private static List<String> warpNames(TacoCommand.Ctx ctx, int arg) {
        List<String> names = new ArrayList<>();
        if (arg == 0) TacoDatabase.warps().forEach(w -> names.add(w.getName()));
        return names;
    }

    private static Loc here(CosmicPlayer player) throws Msg.Fail {
        CosmicLocation here = gameplay().getLocation(player.getUuid()).orElseThrow(() -> Msg.fail("You are not online here."));
        return Loc.of(here);
    }

    /** Homes the player may own; negative means unlimited. */
    private static int maxHomes(TacoCommand.Ctx ctx) {
        if (ctx.has(Perms.HOMES_UNLIMITED)) return -1;
        return TacoEssentials.getConfig().defaultMaxHomes();
    }

    private static void setHome(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        String name = TacoCommand.normalizeName(ctx.require(0, "/sethome <name>"), "home");
        List<TacoDatabase.Named> homes = TacoDatabase.homes(player.getUuid());
        boolean replacing = homes.stream().anyMatch(h -> h.getName().equals(name));
        int max = maxHomes(ctx);
        if (! replacing && max >= 0 && homes.size() >= max) {
            throw Msg.fail(max == 0 ? "You are not allowed to set homes."
                    : "You already have " + homes.size() + "/" + max + " homes. Delete one with /delhome first.");
        }
        TacoDatabase.setHome(player.getUuid(), name, here(player));
        ctx.reply(Msg.success((replacing ? "Moved home " : "Set home ") + name + "."));
    }

    private static void home(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        String name;
        if (ctx.arg(0) != null) {
            name = TacoCommand.normalizeName(ctx.arg(0), "home");
        } else {
            // With no name: the home called "home", or the only home if the player has exactly one.
            List<TacoDatabase.Named> homes = TacoDatabase.homes(player.getUuid());
            if (homes.isEmpty()) throw Msg.fail("You have no homes. Set one with /sethome <name>.");
            if (homes.size() == 1) {
                name = homes.get(0).getName();
            } else if (homes.stream().anyMatch(h -> h.getName().equals(DEFAULT_HOME))) {
                name = DEFAULT_HOME;
            } else {
                List<String> names = new ArrayList<>();
                homes.forEach(h -> names.add(h.getName()));
                throw Msg.fail("You have several homes; pick one: " + String.join(", ", names));
            }
        }
        Optional<Loc> loc = TacoDatabase.home(player.getUuid(), name);
        if (loc.isEmpty()) throw Msg.fail("You have no home named " + name + ".");
        if (! Teleports.teleport(player.getUuid(), loc.get(), true)) {
            throw Msg.fail("Home " + name + " is in a world that no longer exists (" + loc.get().getWorld() + ").");
        }
        ctx.reply(Msg.success("Teleported to home " + name + "."));
    }

    private static void delHome(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        String name = TacoCommand.normalizeName(ctx.require(0, "/delhome <name>"), "home");
        if (! TacoDatabase.deleteHome(player.getUuid(), name)) throw Msg.fail("You have no home named " + name + ".");
        ctx.reply(Msg.success("Deleted home " + name + "."));
    }

    /** {@code /listhomes (player)}; works for offline players. */
    private static void listHomes(TacoCommand.Ctx ctx) throws Msg.Fail {
        String owner;
        String ownerName = null;
        if (ctx.arg(0) != null) {
            if (! ctx.has(Perms.LISTHOMES_OTHERS)) throw Msg.fail("You may only list your own homes.");
            String[] known = TacoCommand.knownPlayer(ctx.arg(0));
            owner = known[0];
            ownerName = known[1];
        } else {
            owner = ctx.player().getUuid();
        }
        List<TacoDatabase.Named> homes = TacoDatabase.homes(owner);
        boolean self = ownerName == null;
        if (homes.isEmpty()) {
            ctx.reply(Msg.info(self ? "You have no homes." : ownerName + " has no homes."));
            return;
        }
        ctx.reply(Msg.info((self ? "Your homes" : ownerName + "'s homes") + " (" + homes.size() + "):"));
        for (TacoDatabase.Named home : homes) {
            ctx.reply(Msg.muted(" - ") + "&f" + home.getName() + Msg.muted(" (" + home.getLocation().describe() + ")"));
        }
    }

    /** {@code /warp (name)}; with no name, lists the warps. */
    private static void warp(TacoCommand.Ctx ctx) throws Msg.Fail {
        if (ctx.arg(0) == null) {
            listWarps(ctx);
            return;
        }
        CosmicPlayer player = ctx.player();
        String name = TacoCommand.normalizeName(ctx.arg(0), "warp");
        Optional<Loc> loc = TacoDatabase.warp(name);
        if (loc.isEmpty()) throw Msg.fail("There is no warp named " + name + ".");
        if (! Teleports.teleport(player.getUuid(), loc.get(), true)) {
            throw Msg.fail("Warp " + name + " is in a world that no longer exists (" + loc.get().getWorld() + ").");
        }
        ctx.reply(Msg.success("Warped to " + name + "."));
    }

    private static void listWarps(TacoCommand.Ctx ctx) {
        List<TacoDatabase.Named> warps = TacoDatabase.warps();
        if (warps.isEmpty()) {
            ctx.reply(Msg.info("There are no warps."));
            return;
        }
        List<String> names = new ArrayList<>();
        warps.forEach(w -> names.add(w.getName()));
        ctx.reply(Msg.info("Warps (" + warps.size() + "): ") + "&f" + String.join(Msg.muted(", ") + "&f", names));
    }

    private static void setWarp(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        String name = TacoCommand.normalizeName(ctx.require(0, "/setwarp <name>"), "warp");
        boolean replacing = TacoDatabase.warp(name).isPresent();
        Loc loc = here(player);
        TacoDatabase.setWarp(name, loc, player.getUuid());
        ctx.reply(Msg.success((replacing ? "Moved warp " : "Set warp ") + name + " to " + loc.describe() + "."));
    }

    private static void delWarp(TacoCommand.Ctx ctx) throws Msg.Fail {
        String name = TacoCommand.normalizeName(ctx.require(0, "/delwarp <name>"), "warp");
        if (! TacoDatabase.deleteWarp(name)) throw Msg.fail("There is no warp named " + name + ".");
        ctx.reply(Msg.success("Deleted warp " + name + "."));
    }
}
