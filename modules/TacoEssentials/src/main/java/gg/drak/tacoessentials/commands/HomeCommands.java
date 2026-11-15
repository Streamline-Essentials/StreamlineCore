package gg.drak.tacoessentials.commands;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.data.Loc;
import gg.drak.tacoessentials.data.TacoDatabase;
import gg.drak.tacoessentials.gui.HomesMenu;
import gg.drak.tacoessentials.gui.WarpsMenu;
import gg.drak.tacoessentials.teleport.Teleports;
import gg.drak.tacoessentials.teleport.Warmups;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.gui.GuiManager;
import singularity.objects.ClickableMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/**
 * Homes ({@code /sethome}, {@code /home}, {@code /homeother}, {@code /delhome}, {@code /homes})
 * and warps ({@code /warp}, {@code /listwarps}, {@code /setwarp}, {@code /delwarp}).
 *
 * <p>Each home sits in a numbered slot (1, 2, ...) and may carry a name; commands take either.
 * How many slots a player has comes from the highest {@code tacoessentials.homes.<N>} they
 * hold, else {@code homes.default-max}; {@link Perms#HOMES_UNLIMITED} removes the cap.</p>
 */
public final class HomeCommands {

    private HomeCommands() {}

    public static List<TacoCommand> create() {
        List<TacoCommand> commands = new ArrayList<>();
        commands.add(new TacoCommand("sethome", HomeCommands::setHome, HomeCommands::ownHomes));
        commands.add(new TacoCommand("home", HomeCommands::home, HomeCommands::ownHomes));
        commands.add(new TacoCommand("homeother", HomeCommands::homeOther, HomeCommands::otherHomes));
        commands.add(new TacoCommand("delhome", HomeCommands::delHome, HomeCommands::ownHomes));
        commands.add(new TacoCommand("homes", HomeCommands::homes,
                (ctx, arg) -> arg == 0 && ctx.has(Perms.HOMES_OTHERS) ? TacoCommand.knownNames() : Collections.emptyList(),
                "listhomes"));

        commands.add(new TacoCommand("warp", HomeCommands::warp, HomeCommands::allowedWarps, "warps"));
        commands.add(new TacoCommand("listwarps", ctx -> listWarps(ctx)));
        commands.add(new TacoCommand("setwarp", HomeCommands::setWarp, HomeCommands::warpNames));
        commands.add(new TacoCommand("delwarp", HomeCommands::delWarp, HomeCommands::warpNames));
        return commands;
    }

    // ---- shared with the homes menu ----

    /** Homes the sender may own; {@link Integer#MAX_VALUE} for unlimited. */
    public static int limit(CosmicSender sender) {
        if (sender.hasPermission(Perms.HOMES_UNLIMITED)) return Integer.MAX_VALUE;
        for (int n = Perms.HOMES_SCAN_LIMIT; n >= 1; n--) {
            if (sender.hasPermission(Perms.homes(n))) return n;
        }
        return Math.max(0, TacoEssentials.getConfig().defaultMaxHomes());
    }

    /** A home by slot number or name (names ignore case). */
    public static Optional<TacoDatabase.Home> find(String uuid, String query) {
        List<TacoDatabase.Home> homes = TacoDatabase.homes(uuid);
        String trimmed = query.trim();
        try {
            int slot = Integer.parseInt(trimmed);
            for (TacoDatabase.Home home : homes) if (home.getSlot() == slot) return Optional.of(home);
        } catch (NumberFormatException ignored) {
            // Not a number; match by name below.
        }
        for (TacoDatabase.Home home : homes) {
            if (home.getName() != null && home.getName().equalsIgnoreCase(trimmed)) return Optional.of(home);
        }
        return Optional.empty();
    }

    /** The lowest slot number the player has not used yet. */
    public static int nextFreeSlot(String uuid) {
        List<TacoDatabase.Home> homes = TacoDatabase.homes(uuid);
        int slot = 1;
        while (true) {
            int candidate = slot;
            if (homes.stream().noneMatch(h -> h.getSlot() == candidate)) return slot;
            slot++;
        }
    }

    /** Checks a home name: letters, digits, spaces, '_' and '-', not only digits (those mean slots). */
    public static String validateHomeName(String raw) throws Msg.Fail {
        String name = raw.trim();
        int max = TacoEssentials.getConfig().homeNameMaxLength();
        if (name.isEmpty() || name.length() > max || ! name.matches("[A-Za-z0-9_\\- ]+") || name.matches("\\d+")) {
            throw Msg.fail("Home names use letters, digits, spaces, '_' and '-' (up to " + max + "), and cannot be only a number.");
        }
        return name;
    }

    /** Teleports the player to one of their own homes, after the countdown. */
    public static void teleportHome(CosmicPlayer player, TacoDatabase.Home home) throws Msg.Fail {
        Loc loc = home.getLocation();
        if (! gameplay().getWorldNames().contains(loc.getWorld())) {
            throw Msg.fail("Home " + home.label() + " is in a world that no longer exists (" + loc.getWorld() + ").");
        }
        Warmups.teleport(player, () -> loc, true, to -> player.sendMessage(Msg.success("Teleported to home " + home.label() + ".")));
    }

    /** Saves a home at the player's position in {@code slot}. */
    public static void setHomeHere(CosmicPlayer player, int slot, String name) throws Msg.Fail {
        TacoDatabase.setHome(player.getUuid(), slot, name, here(player));
    }

    // ---- commands ----

    private static List<String> ownHomes(TacoCommand.Ctx ctx, int arg) {
        List<String> names = new ArrayList<>();
        if (arg != 0 || ! ctx.isPlayer()) return names;
        TacoDatabase.homes(ctx.sender().getUuid()).forEach(h -> names.add(h.query()));
        return names;
    }

    private static List<String> otherHomes(TacoCommand.Ctx ctx, int arg) {
        if (arg == 0) return TacoCommand.knownNames();
        List<String> names = new ArrayList<>();
        if (arg != 1 || ctx.arg(0) == null) return names;
        try {
            TacoDatabase.homes(TacoCommand.knownPlayer(ctx.arg(0))[0]).forEach(h -> names.add(h.query()));
        } catch (Msg.Fail ignored) {
            // Unknown player: nothing to offer.
        }
        return names;
    }

    private static Loc here(CosmicPlayer player) throws Msg.Fail {
        CosmicLocation here = gameplay().getLocation(player.getUuid()).orElseThrow(() -> Msg.fail("You are not online here."));
        return Loc.of(here);
    }

    /** {@code /sethome (name)}: moves the home with that name, or saves one in the next free slot. */
    private static void setHome(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        String name = ctx.rest(0) == null ? null : validateHomeName(ctx.rest(0));
        if (name != null) {
            Optional<TacoDatabase.Home> existing = find(player.getUuid(), name);
            if (existing.isPresent() && existing.get().getName() != null) {
                setHomeHere(player, existing.get().getSlot(), existing.get().getName());
                ctx.reply(Msg.success("Moved home " + existing.get().label() + " here."));
                return;
            }
        }
        int slot = nextFreeSlot(player.getUuid());
        int limit = limit(player);
        if (slot > limit) {
            throw Msg.fail(limit == 0 ? "You are not allowed to set homes."
                    : "You have reached your limit of " + limit + " home" + (limit == 1 ? "" : "s") + ". Delete one with /delhome first.");
        }
        setHomeHere(player, slot, name);
        ctx.reply(Msg.success("Set home " + (name == null ? "#" + slot : slot + " (" + name + ")") + "."));
    }

    /**
     * {@code /home <home>} by number or name. With none: the only home when there is one,
     * otherwise the homes menu.
     */
    private static void home(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        String query = ctx.rest(0);
        if (query == null) {
            List<TacoDatabase.Home> homes = TacoDatabase.homes(player.getUuid());
            if (homes.isEmpty()) throw Msg.fail("You have no homes. Set one with /sethome (name).");
            if (homes.size() == 1) {
                teleportHome(player, homes.get(0));
                return;
            }
            showHomes(ctx, player.getUuid(), null);
            return;
        }
        TacoDatabase.Home home = find(player.getUuid(), query).orElseThrow(() -> Msg.fail("You have no home " + query + "."));
        teleportHome(player, home);
    }

    /** {@code /homeother <player> <home>}: a staff teleport, so it skips the countdown. */
    private static void homeOther(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        String usage = "/homeother <player> <home>";
        String[] owner = TacoCommand.knownPlayer(ctx.require(0, usage));
        ctx.require(1, usage);
        String query = ctx.rest(1);
        TacoDatabase.Home home = find(owner[0], query).orElseThrow(() -> Msg.fail(owner[1] + " has no home " + query + "."));
        if (! Teleports.teleport(player.getUuid(), home.getLocation(), true)) {
            throw Msg.fail("That home is in a world that no longer exists (" + home.getLocation().getWorld() + ").");
        }
        ctx.reply(Msg.success("Teleported to " + owner[1] + "'s home " + home.label() + "."));
    }

    /** {@code /delhome <home>} by number or name. */
    private static void delHome(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        ctx.require(0, "/delhome <home>");
        String query = ctx.rest(0);
        TacoDatabase.Home home = find(player.getUuid(), query).orElseThrow(() -> Msg.fail("You have no home " + query + "."));
        TacoDatabase.deleteHome(player.getUuid(), home.getSlot());
        ctx.reply(Msg.success("Deleted home " + home.label() + "."));
    }

    /** {@code /homes (player)}: the homes menu, or a clickable list where menus cannot open. */
    private static void homes(TacoCommand.Ctx ctx) throws Msg.Fail {
        if (ctx.arg(0) == null) {
            showHomes(ctx, ctx.player().getUuid(), null);
            return;
        }
        String[] known = TacoCommand.knownPlayer(ctx.arg(0));
        boolean self = ctx.isPlayer() && ctx.sender().getUuid().equals(known[0]);
        if (! self && ! ctx.has(Perms.HOMES_OTHERS)) throw Msg.fail("You may only view your own homes.");
        showHomes(ctx, known[0], self ? null : known[1]);
    }

    /** @param ownerName {@code null} for the runner's own homes */
    private static void showHomes(TacoCommand.Ctx ctx, String owner, String ownerName) {
        if (ctx.isPlayer() && GuiManager.isAvailable()) {
            new HomesMenu(owner, ownerName).open((CosmicPlayer) ctx.sender());
            return;
        }
        List<TacoDatabase.Home> homes = TacoDatabase.homes(owner);
        boolean self = ownerName == null;
        if (homes.isEmpty()) {
            ctx.reply(Msg.info(self ? "You have no homes." : ownerName + " has no homes."));
            return;
        }
        String limit = self && ctx.isPlayer() ? "/" + (limit(ctx.sender()) == Integer.MAX_VALUE ? "∞" : String.valueOf(limit(ctx.sender()))) : "";
        ctx.reply(Msg.info((self ? "Your homes" : ownerName + "'s homes") + " (" + homes.size() + limit + "):"));
        for (TacoDatabase.Home home : homes) {
            String command = self ? "/home " + home.query() : "/homeother " + ownerName + " " + home.query();
            new ClickableMessage()
                    .text(Msg.muted(" - ") + "&f" + home.label() + Msg.muted(" (" + home.getLocation().describe() + ")"))
                    .hover(Msg.info("Click to teleport")).run(command)
                    .send(ctx.sender());
        }
    }

    // ---- warps ----

    private static List<String> warpNames(TacoCommand.Ctx ctx, int arg) {
        List<String> names = new ArrayList<>();
        if (arg == 0) TacoDatabase.warps().forEach(w -> names.add(w.getName()));
        return names;
    }

    private static List<String> allowedWarps(TacoCommand.Ctx ctx, int arg) {
        List<String> names = new ArrayList<>();
        if (arg == 0) allowedWarps(ctx.sender()).forEach(w -> names.add(w.getName()));
        return names;
    }

    /** The warps the sender may use: open ones, and restricted ones whose permission they hold. */
    public static List<TacoDatabase.Named> allowedWarps(CosmicSender sender) {
        Map<String, String> perms = TacoDatabase.warpPermissions();
        List<TacoDatabase.Named> out = new ArrayList<>();
        for (TacoDatabase.Named warp : TacoDatabase.warps()) {
            String permission = perms.get(warp.getName());
            if (permission == null || sender.hasPermission(permission)) out.add(warp);
        }
        return out;
    }

    /** Warps the player to the named warp, after the countdown. */
    public static void warpTo(CosmicPlayer player, String name) throws Msg.Fail {
        Optional<Loc> loc = TacoDatabase.warp(name);
        if (loc.isEmpty()) throw Msg.fail("There is no warp named " + name + ".");
        String permission = TacoDatabase.warpPermissions().get(name);
        if (permission != null && ! player.hasPermission(permission)) throw Msg.fail("You may not use warp " + name + ".");
        if (! gameplay().getWorldNames().contains(loc.get().getWorld())) {
            throw Msg.fail("Warp " + name + " is in a world that no longer exists (" + loc.get().getWorld() + ").");
        }
        Warmups.teleport(player, loc::get, true, to -> player.sendMessage(Msg.success("Warped to " + name + ".")));
    }

    /** {@code /warp (name)}; with no name, the warps menu or list. */
    private static void warp(TacoCommand.Ctx ctx) throws Msg.Fail {
        if (ctx.arg(0) == null) {
            if (ctx.isPlayer() && GuiManager.isAvailable()) {
                new WarpsMenu().open((CosmicPlayer) ctx.sender());
                return;
            }
            listWarps(ctx);
            return;
        }
        warpTo(ctx.player(), TacoCommand.normalizeName(ctx.arg(0), "warp"));
    }

    private static void listWarps(TacoCommand.Ctx ctx) {
        List<TacoDatabase.Named> warps = allowedWarps(ctx.sender());
        if (warps.isEmpty()) {
            ctx.reply(Msg.info("There are no warps."));
            return;
        }
        ClickableMessage line = new ClickableMessage().text(Msg.info("Warps (" + warps.size() + "): "));
        for (int i = 0; i < warps.size(); i++) {
            if (i > 0) line.text(Msg.muted(", "));
            String name = warps.get(i).getName();
            line.text("&f" + name).hover(Msg.info("Click to warp to " + name)).run("/warp " + name);
        }
        line.send(ctx.sender());
    }

    /** {@code /setwarp <name> (permission)}: (permission) limits who may use it; leaving it out opens it to everyone. */
    private static void setWarp(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        String name = TacoCommand.normalizeName(ctx.require(0, "/setwarp <name> (permission)"), "warp");
        String permission = ctx.arg(1);
        boolean replacing = TacoDatabase.warp(name).isPresent();
        Loc loc = here(player);
        TacoDatabase.setWarp(name, loc, player.getUuid(), permission);
        ctx.reply(Msg.success((replacing ? "Moved warp " : "Set warp ") + name + " to " + loc.describe()
                + (permission == null ? "." : ", for players with " + permission + ".")));
    }

    private static void delWarp(TacoCommand.Ctx ctx) throws Msg.Fail {
        String name = TacoCommand.normalizeName(ctx.require(0, "/delwarp <name>"), "warp");
        if (! TacoDatabase.deleteWarp(name)) throw Msg.fail("There is no warp named " + name + ".");
        ctx.reply(Msg.success("Deleted warp " + name + "."));
    }
}
