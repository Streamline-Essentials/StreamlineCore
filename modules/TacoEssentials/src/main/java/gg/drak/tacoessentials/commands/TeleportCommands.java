package gg.drak.tacoessentials.commands;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.data.Loc;
import gg.drak.tacoessentials.data.PendingActions;
import gg.drak.tacoessentials.data.RtpConfig;
import gg.drak.tacoessentials.data.Sessions;
import gg.drak.tacoessentials.data.TacoDatabase;
import gg.drak.tacoessentials.teleport.Teleports;
import gg.drak.tacoessentials.teleport.TpaManager;
import gg.drak.tacoessentials.teleport.Warmups;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.RandomTeleportArea;
import singularity.objects.ClickableMessage;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/**
 * {@code /tpa}, {@code /tpahere}, {@code /tpaccept}, {@code /tpdeny}, {@code /tpacancel},
 * {@code /tpauto}, {@code /back}, {@code /dback}, {@code /rtp}, {@code /spawn} and the spawn
 * setters. Player teleports here wait out the {@link Warmups stand-still countdown}.
 */
public final class TeleportCommands {

    private TeleportCommands() {}

    public static List<TacoCommand> create() {
        List<TacoCommand> commands = new ArrayList<>();
        commands.add(new TacoCommand("tpa", ctx -> request(ctx, TpaManager.Kind.TO_TARGET), TeleportCommands::onlineOthers));
        commands.add(new TacoCommand("tpahere", ctx -> request(ctx, TpaManager.Kind.HERE), TeleportCommands::onlineOthers));
        commands.add(new TacoCommand("tpaccept", ctx -> respond(ctx, true), TeleportCommands::incomingNames));
        commands.add(new TacoCommand("tpdeny", ctx -> respond(ctx, false), TeleportCommands::incomingNames, "tpadeny"));
        commands.add(new TacoCommand("tpacancel", TeleportCommands::cancel, TeleportCommands::outgoingNames));
        commands.add(new TacoCommand("tpauto", TeleportCommands::auto));
        commands.add(new TacoCommand("back", ctx -> back(ctx, false), (ctx, arg) -> backCompletions(ctx, arg, false)));
        commands.add(new TacoCommand("dback", ctx -> back(ctx, true), (ctx, arg) -> backCompletions(ctx, arg, true)));
        commands.add(new TacoCommand("rtp", TeleportCommands::rtp, TeleportCommands::rtpWorlds));
        commands.add(new TacoCommand("spawn", TeleportCommands::spawn));
        commands.add(new TacoCommand("setspawn", ctx -> setServerLocation(ctx, TacoDatabase.SPAWN, "Spawn")));
        commands.add(new TacoCommand("setfirstspawn", ctx -> setServerLocation(ctx, TacoDatabase.FIRST_SPAWN, "First-join spawn")));
        return commands;
    }

    // ---- teleport requests ----

    private static List<String> onlineOthers(TacoCommand.Ctx ctx, int arg) {
        if (arg != 0) return Collections.emptyList();
        List<String> names = TacoCommand.onlineNames();
        if (ctx.isPlayer()) names.remove(((CosmicPlayer) ctx.sender()).getCurrentName());
        return names;
    }

    private static List<String> incomingNames(TacoCommand.Ctx ctx, int arg) {
        List<String> names = new ArrayList<>();
        if (arg != 0 || ! ctx.isPlayer()) return names;
        TpaManager.incoming(ctx.sender().getUuid()).forEach(r -> names.add(r.getSenderName()));
        return names;
    }

    private static List<String> outgoingNames(TacoCommand.Ctx ctx, int arg) {
        List<String> names = new ArrayList<>();
        if (arg != 0 || ! ctx.isPlayer()) return names;
        TpaManager.outgoing(ctx.sender().getUuid()).forEach(r -> names.add(r.getTargetName()));
        return names;
    }

    private static void request(TacoCommand.Ctx ctx, TpaManager.Kind kind) throws Msg.Fail {
        CosmicPlayer sender = ctx.player();
        String usage = kind == TpaManager.Kind.TO_TARGET ? "/tpa <player>" : "/tpahere <player>";
        CosmicPlayer target = TacoCommand.onlinePlayer(ctx.require(0, usage));
        if (target.getUuid().equals(sender.getUuid())) throw Msg.fail("You cannot send a teleport request to yourself.");

        TpaManager.Request req = TpaManager.create(sender, target, kind);
        ctx.reply(Msg.success("Teleport request sent to " + target.getCurrentName() + ".")
                + Msg.muted(" Cancel it with /tpacancel."));

        if (Sessions.get(target.getUuid()).isTpAuto()) {
            target.sendMessage(Msg.muted("Accepting " + sender.getCurrentName() + "'s request automatically (/tpauto)."));
            accept(target, req);
            return;
        }

        String what = kind == TpaManager.Kind.TO_TARGET
                ? sender.getCurrentName() + " wants to teleport to you."
                : sender.getCurrentName() + " wants you to teleport to them.";
        target.sendMessage(Msg.info(what));
        String name = sender.getCurrentName();
        new ClickableMessage()
                .text("&a&l[Accept]").hover(Msg.success("Click to accept")).run("/tpaccept " + name)
                .text(" ")
                .text("&c&l[Deny]").hover(Msg.error("Click to deny")).run("/tpdeny " + name)
                .text(Msg.muted(" Expires in " + TacoEssentials.getConfig().tpaTimeoutSeconds() + "s."))
                .send(target);
    }

    /** {@code /tpaccept (player)} and {@code /tpdeny (player)}: the request from (player), or the newest. */
    private static void respond(TacoCommand.Ctx ctx, boolean accept) throws Msg.Fail {
        CosmicPlayer me = ctx.player();
        String from = ctx.arg(0);
        TpaManager.Request req = TpaManager.find(me.getUuid(), from);
        if (req == null) {
            throw Msg.fail(from == null ? "You have no pending teleport requests." : "You have no pending teleport request from " + from + ".");
        }
        if (accept) {
            accept(me, req);
            return;
        }
        TpaManager.remove(req);
        ctx.reply(Msg.info("Denied " + req.getSenderName() + "'s teleport request."));
        online(req.getSender()).ifPresent(sender -> sender.sendMessage(Msg.error(me.getCurrentName() + " denied your teleport request.")));
    }

    /** Starts the traveller's countdown; the destination is read when it ends, so it follows the other player. */
    private static void accept(CosmicPlayer me, TpaManager.Request req) {
        TpaManager.remove(req);
        Optional<CosmicPlayer> sender = online(req.getSender());
        if (sender.isEmpty() || gameplay().getLocation(req.getSender()).isEmpty()) {
            me.sendMessage(Msg.error(req.getSenderName() + " is no longer online."));
            return;
        }
        boolean senderTravels = req.getKind() == TpaManager.Kind.TO_TARGET;
        CosmicPlayer traveller = senderTravels ? sender.get() : me;
        CosmicPlayer destination = senderTravels ? me : sender.get();

        me.sendMessage(Msg.success("Accepted " + sender.get().getCurrentName() + "'s teleport request."));
        sender.get().sendMessage(Msg.success(me.getCurrentName() + " accepted your teleport request."));
        Warmups.teleport(traveller, () -> gameplay().getLocation(destination.getUuid()).map(Loc::of)
                        .orElseThrow(() -> Msg.fail(destination.getCurrentName() + " is no longer online.")),
                true, to -> traveller.sendMessage(Msg.success("Teleported to " + destination.getCurrentName() + ".")));
    }

    /** {@code /tpacancel (player)}: withdraws the runner's request to (player), or all of them. */
    private static void cancel(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer me = ctx.player();
        String only = ctx.arg(0);
        int cancelled = 0;
        for (TpaManager.Request req : TpaManager.outgoing(me.getUuid())) {
            if (only != null && ! req.getTargetName().equalsIgnoreCase(only)) continue;
            if (! TpaManager.remove(req)) continue;
            cancelled++;
            online(req.getTarget()).ifPresent(target -> target.sendMessage(Msg.muted(me.getCurrentName() + " cancelled their teleport request.")));
        }
        if (cancelled == 0) {
            throw Msg.fail(only == null ? "You have no pending teleport requests to cancel." : "You have no pending teleport request to " + only + ".");
        }
        ctx.reply(Msg.success("Cancelled " + cancelled + " teleport request" + (cancelled == 1 ? "" : "s") + "."));
    }

    /** {@code /tpauto}: toggles accepting every teleport request automatically; saved per player. */
    private static void auto(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer me = ctx.player();
        boolean on = ! Sessions.get(me.getUuid()).isTpAuto();
        Sessions.setTpAuto(me.getUuid(), on);
        ctx.reply(on ? Msg.success("You now accept teleport requests automatically.")
                : Msg.info("You no longer accept teleport requests automatically."));
    }

    private static Optional<CosmicPlayer> online(String uuid) {
        return UserUtils.getPlayer(uuid).filter(CosmicPlayer::isOnline);
    }

    // ---- /back and /dback ----

    /**
     * {@code /back (<player> (target)|<n> <player> (target))} and {@code /dback} with the same
     * arguments, plus {@code /back <n>}. {@code n} picks how far back (1 is the newest),
     * {@code player} whose history is used and {@code target} who is moved (the runner by
     * default). Every teleport records the spot left behind, so {@code /back} twice returns the
     * player. Only the plain form waits out the countdown; an offline target is moved when they
     * next join.
     */
    private static void back(TacoCommand.Ctx ctx, boolean death) throws Msg.Fail {
        String label = death ? "dback" : "back";
        String usage = "/" + label + " (<player> (target)|<n> <player> (target))";
        String kind = death ? TacoDatabase.DEATH : TacoDatabase.BACK;
        int index = 0;
        int depth = 1;
        if (ctx.arg(0) != null && isDepth(ctx.arg(0))) {
            depth = Integer.parseInt(ctx.arg(0));
            index = 1;
        }
        int remaining = ctx.count() - index;
        if (remaining > 2) throw Msg.fail("Usage: " + usage);
        String sourceName = remaining >= 1 ? ctx.arg(index) : null;
        String targetName = remaining == 2 ? ctx.arg(index + 1) : null;

        TacoCommand.Target source = sourceName == null ? TacoCommand.self(ctx)
                : named(ctx, sourceName, death ? Perms.DBACK_OTHERS : Perms.BACK_OTHERS, "history");
        TacoCommand.Target target = targetName == null ? TacoCommand.self(ctx)
                : named(ctx, targetName, death ? Perms.DBACK_TELEPORT_OTHERS : Perms.BACK_TELEPORT_OTHERS, "teleport");

        boolean own = source.isSelf(ctx);
        String whose = own ? "You have" : source.name + " has";
        Optional<Loc> loc = TacoDatabase.history(source.uuid, kind, depth);
        if (loc.isEmpty()) {
            int count = TacoDatabase.historyCount(source.uuid, kind);
            String noun = death ? "recorded death" : "previous location";
            throw Msg.fail(count == 0 ? whose + " no " + noun + "s." : whose + " only " + count + " " + noun + (count == 1 ? "" : "s") + ".");
        }
        Loc to = loc.get();
        if (! gameplay().getWorldNames().contains(to.getWorld())) {
            throw Msg.fail("That location is in a world that no longer exists (" + to.getWorld() + ").");
        }
        String which = death ? (depth == 1 ? "latest death" : "death #" + depth) : (depth == 1 ? "previous location" : "location #" + depth);
        String what = (own ? "your " : source.name + "'s ") + which + " (" + to.describe() + ")";

        if (sourceName == null && targetName == null) {
            Warmups.teleport(target.online, () -> to, true, arrived -> target.online.sendMessage(Msg.success("Returned to " + what + ".")));
            return;
        }
        if (target.online == null) {
            PendingActions.teleport(target.uuid, to);
            ctx.reply(Msg.success(target.name + " is offline; they will be sent to " + what + " when they next join."));
            return;
        }
        if (! Teleports.teleport(target.uuid, to, true)) throw Msg.fail(target.name + " is not online here.");
        target.online.sendMessage(Msg.success("Teleported to " + what + "."));
        if (! target.isSelf(ctx)) ctx.reply(Msg.success("Sent " + target.name + " to " + what + "."));
    }

    private static boolean isDepth(String text) {
        try {
            return Integer.parseInt(text) >= 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** A named player for {@code /back}: the runner when it is their own name, else needs {@code permission}. */
    private static TacoCommand.Target named(TacoCommand.Ctx ctx, String name, String permission, String what) throws Msg.Fail {
        if (ctx.isPlayer() && name.equalsIgnoreCase(((CosmicPlayer) ctx.sender()).getCurrentName())) return TacoCommand.self(ctx);
        if (! ctx.has(permission)) {
            throw Msg.fail(what.equals("history") ? "You may only use your own history." : "You may only teleport yourself.");
        }
        return TacoCommand.target(name);
    }

    private static List<String> backCompletions(TacoCommand.Ctx ctx, int arg, boolean death) {
        List<String> out = new ArrayList<>();
        boolean depthLed = ctx.arg(0) != null && isDepth(ctx.arg(0)) && arg > 0;
        if (arg == 0) {
            int size = death ? TacoEssentials.getConfig().deathHistorySize() : TacoEssentials.getConfig().backHistorySize();
            for (int i = 1; i <= size; i++) out.add(String.valueOf(i));
        }
        int nameIndex = arg - (depthLed ? 1 : 0);
        boolean others = ctx.has(death ? Perms.DBACK_OTHERS : Perms.BACK_OTHERS)
                || ctx.has(death ? Perms.DBACK_TELEPORT_OTHERS : Perms.BACK_TELEPORT_OTHERS);
        if (others && (nameIndex == 0 || nameIndex == 1)) out.addAll(TacoCommand.onlineNames());
        return out;
    }

    // ---- /rtp ----

    /** {@code /rtp (world)}: a random safe spot in (world), or in the runner's own world. */
    private static void rtp(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        Sessions.Session session = Sessions.get(player.getUuid());
        long cooldownMs = TacoEssentials.getConfig().rtpCooldownSeconds() * 1000L;
        long waited = System.currentTimeMillis() - session.getLastRtpMillis();
        if (waited < cooldownMs && ! ctx.has(Perms.RTP_BYPASS_COOLDOWN)) {
            throw Msg.fail("You can use /rtp again in " + TacoCommand.duration((cooldownMs - waited + 999) / 1000) + ".");
        }

        RtpConfig rtpConfig = TacoEssentials.getRtpConfig();
        if (! rtpConfig.isEnabled()) throw Msg.fail("Random teleport is disabled.");
        CosmicLocation here = gameplay().getLocation(player.getUuid()).orElseThrow(() -> Msg.fail("You are not online here."));
        String world = ctx.arg(0) == null ? here.getWorldName() : TacoCommand.resolveWorld(ctx.arg(0));
        RandomTeleportArea area = rtpConfig.area(world)
                .orElseThrow(() -> Msg.fail("Random teleport is disabled in " + (ctx.arg(0) == null ? "this world." : Loc.shortWorld(world) + ".")));
        Optional<CosmicLocation> spot = gameplay().findRandomSafeLocation(area, TacoEssentials.getConfig().rtpMaxAttempts());
        if (spot.isEmpty()) throw Msg.fail("Could not find a safe location. Try again.");
        session.setLastRtpMillis(System.currentTimeMillis());

        Loc to = new Loc(spot.get().getWorldName(), spot.get().getX(), spot.get().getY(), spot.get().getZ(),
                here.getYaw(), here.getPitch());
        Warmups.teleport(player, () -> to, true,
                arrived -> player.sendMessage(Msg.success(String.format("Teleported to %.0f, %.0f, %.0f.", to.getX(), to.getY(), to.getZ()))));
    }

    private static List<String> rtpWorlds(TacoCommand.Ctx ctx, int arg) {
        List<String> out = new ArrayList<>();
        if (arg != 0) return out;
        for (String world : gameplay().getWorldNames()) {
            if (TacoEssentials.getRtpConfig().area(world).isPresent()) out.add(Loc.shortWorld(world));
        }
        return out;
    }

    // ---- spawn ----

    /** The {@code /setspawn} location when one is set, otherwise the world spawn. */
    private static void spawn(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        Optional<Loc> custom = TacoDatabase.serverLocation(TacoDatabase.SPAWN);
        if (custom.isPresent() && ! gameplay().getWorldNames().contains(custom.get().getWorld())) {
            throw Msg.fail("The spawn is in a world that no longer exists (" + custom.get().getWorld() + ").");
        }
        Warmups.teleport(player, () -> {
            if (custom.isPresent()) return custom.get();
            return Loc.of(gameplay().getWorldSpawn().orElseThrow(() -> Msg.fail("The server has no spawn.")));
        }, true, to -> player.sendMessage(Msg.success("Teleported to spawn.")));
    }

    private static void setServerLocation(TacoCommand.Ctx ctx, String key, String label) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        CosmicLocation here = gameplay().getLocation(player.getUuid()).orElseThrow(() -> Msg.fail("You are not online here."));
        Loc loc = Loc.of(here);
        TacoDatabase.setServerLocation(key, loc, player.getUuid());
        ctx.reply(Msg.success(label + " set to " + loc.describe() + "."));
    }
}
