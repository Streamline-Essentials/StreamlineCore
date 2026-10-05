package gg.drak.tacoessentials.commands;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.data.Loc;
import gg.drak.tacoessentials.data.RtpConfig;
import gg.drak.tacoessentials.data.Sessions;
import gg.drak.tacoessentials.data.TacoDatabase;
import gg.drak.tacoessentials.teleport.Teleports;
import gg.drak.tacoessentials.teleport.TpaManager;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.RandomTeleportArea;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/** {@code /tpa}, {@code /tpahere}, {@code /tpaccept}, {@code /tpadeny}, {@code /back}, {@code /dback}, {@code /rtp}, {@code /spawn} and the spawn setters. */
public final class TeleportCommands {

    private TeleportCommands() {}

    public static List<TacoCommand> create() {
        List<TacoCommand> commands = new ArrayList<>();
        commands.add(new TacoCommand("tpa", ctx -> request(ctx, TpaManager.Kind.TO_TARGET), TeleportCommands::onlineOthers));
        commands.add(new TacoCommand("tpahere", ctx -> request(ctx, TpaManager.Kind.HERE), TeleportCommands::onlineOthers));
        commands.add(new TacoCommand("tpaccept", ctx -> respond(ctx, true), TeleportCommands::incomingIds));
        commands.add(new TacoCommand("tpadeny", ctx -> respond(ctx, false), TeleportCommands::incomingIds));
        commands.add(new TacoCommand("back", TeleportCommands::back));
        commands.add(new TacoCommand("dback", TeleportCommands::deathBack,
                (ctx, arg) -> arg == 1 && ctx.has(Perms.DBACK_OTHERS) ? TacoCommand.knownNames() : Collections.emptyList()));
        commands.add(new TacoCommand("rtp", TeleportCommands::rtp));
        commands.add(new TacoCommand("spawn", TeleportCommands::spawn));
        commands.add(new TacoCommand("setspawn", ctx -> setServerLocation(ctx, TacoDatabase.SPAWN, "Spawn")));
        commands.add(new TacoCommand("setfirstspawn", ctx -> setServerLocation(ctx, TacoDatabase.FIRST_SPAWN, "First-join spawn")));
        return commands;
    }

    private static List<String> onlineOthers(TacoCommand.Ctx ctx, int arg) {
        if (arg != 0) return Collections.emptyList();
        List<String> names = TacoCommand.onlineNames();
        if (ctx.isPlayer()) names.remove(((CosmicPlayer) ctx.sender()).getCurrentName());
        return names;
    }

    private static List<String> incomingIds(TacoCommand.Ctx ctx, int arg) {
        List<String> ids = new ArrayList<>();
        if (arg != 0 || ! ctx.isPlayer()) return ids;
        TpaManager.incoming(ctx.sender().getUuid()).forEach(r -> ids.add(String.valueOf(r.getId())));
        return ids;
    }

    private static void request(TacoCommand.Ctx ctx, TpaManager.Kind kind) throws Msg.Fail {
        CosmicPlayer sender = ctx.player();
        String usage = kind == TpaManager.Kind.TO_TARGET ? "/tpa <player>" : "/tpahere <player>";
        CosmicPlayer target = TacoCommand.onlinePlayer(ctx.require(0, usage));
        if (target.getUuid().equals(sender.getUuid())) throw Msg.fail("You cannot send a teleport request to yourself.");

        TpaManager.Request req = TpaManager.create(sender.getUuid(), target.getUuid(), kind);
        String what = kind == TpaManager.Kind.TO_TARGET
                ? sender.getCurrentName() + " wants to teleport to you."
                : sender.getCurrentName() + " wants you to teleport to them.";
        target.sendMessage(Msg.info(what + " ") + Msg.muted("(#" + req.getId() + ")"));
        target.sendMessage(Msg.info("Type ") + "&a/tpaccept " + req.getId() + Msg.info(" or ") + "&c/tpadeny " + req.getId()
                + Msg.muted(". Expires in " + TacoEssentials.getConfig().tpaTimeoutSeconds() + "s."));
        ctx.reply(Msg.success("Teleport request #" + req.getId() + " sent to " + target.getCurrentName() + "."));
    }

    private static void respond(TacoCommand.Ctx ctx, boolean accept) throws Msg.Fail {
        CosmicPlayer me = ctx.player();
        TpaManager.Request req;
        if (ctx.arg(0) == null) {
            List<TpaManager.Request> incoming = TpaManager.incoming(me.getUuid());
            if (incoming.isEmpty()) throw Msg.fail("You have no pending teleport requests.");
            req = incoming.get(0);
        } else {
            int id = ctx.requireInt(0, accept ? "/tpaccept (id)" : "/tpadeny (id)");
            req = TpaManager.get(id);
            if (req == null || ! req.getTarget().equals(me.getUuid())) throw Msg.fail("No pending teleport request #" + id + " for you.");
        }
        TpaManager.remove(req.getId());

        Optional<CosmicPlayer> sender = UserUtils.getPlayer(req.getSender()).filter(CosmicPlayer::isOnline);
        if (sender.isEmpty() || gameplay().getLocation(req.getSender()).isEmpty()) {
            throw Msg.fail("That player is no longer online.");
        }

        if (! accept) {
            sender.get().sendMessage(Msg.error(me.getCurrentName() + " denied your teleport request."));
            ctx.reply(Msg.info("Denied teleport request #" + req.getId() + "."));
            return;
        }

        // Positions are read now, at accept time, so the traveller lands where the other player currently is.
        boolean senderTravels = req.getKind() == TpaManager.Kind.TO_TARGET;
        CosmicPlayer traveller = senderTravels ? sender.get() : me;
        CosmicPlayer destination = senderTravels ? me : sender.get();
        CosmicLocation there = gameplay().getLocation(destination.getUuid())
                .orElseThrow(() -> Msg.fail("That player is no longer online."));
        if (! Teleports.teleport(traveller.getUuid(), Loc.of(there), true)) throw Msg.fail("The teleport failed.");

        traveller.sendMessage(Msg.success("Teleported to " + destination.getCurrentName() + "."));
        if (senderTravels) ctx.reply(Msg.success("Accepted teleport request #" + req.getId() + "."));
        else sender.get().sendMessage(Msg.success(me.getCurrentName() + " accepted your teleport request."));
    }

    private static void back(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        // Entries whose world no longer exists are discarded until a usable one is found.
        while (true) {
            Optional<Loc> loc = TacoDatabase.pop(player.getUuid(), TacoDatabase.BACK);
            if (loc.isEmpty()) throw Msg.fail("You have no previous location to return to.");
            if (Teleports.teleport(player.getUuid(), loc.get(), false)) {
                ctx.reply(Msg.success("Returned to your previous location (" + loc.get().describe() + ")."));
                return;
            }
        }
    }

    /** {@code /dback <n> (player)}: the nth most recent death of the runner or of {@code (player)}. */
    private static void deathBack(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        int n = ctx.requireInt(0, "/dback <n> (player)");
        if (n < 1) throw Msg.fail("<n> starts at 1, the latest death.");

        String owner = player.getUuid();
        String ownerName = null;
        if (ctx.arg(1) != null) {
            if (! ctx.has(Perms.DBACK_OTHERS)) throw Msg.fail("You may only use your own death history.");
            String[] known = TacoCommand.knownPlayer(ctx.arg(1));
            owner = known[0];
            ownerName = known[1];
        }

        String whose = ownerName == null ? "You have" : ownerName + " has";
        Optional<Loc> loc = TacoDatabase.history(owner, TacoDatabase.DEATH, n);
        if (loc.isEmpty()) {
            int count = TacoDatabase.historyCount(owner, TacoDatabase.DEATH);
            throw Msg.fail(count == 0 ? whose + " no recorded deaths."
                    : whose + " only " + count + " recorded death" + (count == 1 ? "" : "s") + ".");
        }
        if (! Teleports.teleport(player.getUuid(), loc.get(), true)) {
            throw Msg.fail("That death was in a world that no longer exists (" + loc.get().getWorld() + ").");
        }
        String which = n == 1 ? "latest death" : "death #" + n;
        String label = ownerName == null ? "your " + which : ownerName + "'s " + which;
        ctx.reply(Msg.success("Teleported to " + label + " (" + loc.get().describe() + ")."));
    }

    private static void rtp(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        Sessions.Session session = Sessions.get(player.getUuid());
        long cooldownMs = TacoEssentials.getConfig().rtpCooldownSeconds() * 1000L;
        long waited = System.currentTimeMillis() - session.getLastRtpMillis();
        if (waited < cooldownMs && ! ctx.has(Perms.RTP_BYPASS_COOLDOWN)) {
            throw Msg.fail("You can use /rtp again in " + ((cooldownMs - waited + 999) / 1000) + "s.");
        }

        RtpConfig rtpConfig = TacoEssentials.getRtpConfig();
        if (! rtpConfig.isEnabled()) throw Msg.fail("Random teleport is disabled.");
        CosmicLocation here = gameplay().getLocation(player.getUuid()).orElseThrow(() -> Msg.fail("You are not online here."));
        RandomTeleportArea area = rtpConfig.area(here.getWorldName())
                .orElseThrow(() -> Msg.fail("Random teleport is disabled in this world."));
        Optional<CosmicLocation> spot = gameplay().findRandomSafeLocation(area, TacoEssentials.getConfig().rtpMaxAttempts());
        if (spot.isEmpty()) throw Msg.fail("Could not find a safe location. Try again.");
        session.setLastRtpMillis(System.currentTimeMillis());

        Loc to = new Loc(spot.get().getWorldName(), spot.get().getX(), spot.get().getY(), spot.get().getZ(),
                here.getYaw(), here.getPitch());
        Teleports.teleport(player.getUuid(), to, true);
        ctx.reply(Msg.success(String.format("Teleported to %.0f, %.0f, %.0f.", to.getX(), to.getY(), to.getZ())));
    }

    /** The {@code /setspawn} location when one is set, otherwise the world spawn. */
    private static void spawn(TacoCommand.Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        Optional<Loc> custom = TacoDatabase.serverLocation(TacoDatabase.SPAWN);
        if (custom.isPresent()) {
            if (! Teleports.teleport(player.getUuid(), custom.get(), true)) {
                throw Msg.fail("The spawn is in a world that no longer exists (" + custom.get().getWorld() + ").");
            }
        } else {
            CosmicLocation spawn = gameplay().getWorldSpawn().orElseThrow(() -> Msg.fail("The server has no spawn."));
            Teleports.teleport(player.getUuid(), Loc.of(spawn), true);
        }
        ctx.reply(Msg.success("Teleported to spawn."));
    }

    private static void setServerLocation(TacoCommand.Ctx ctx, String key, String label) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        CosmicLocation here = gameplay().getLocation(player.getUuid()).orElseThrow(() -> Msg.fail("You are not online here."));
        Loc loc = Loc.of(here);
        TacoDatabase.setServerLocation(key, loc, player.getUuid());
        ctx.reply(Msg.success(label + " set to " + loc.describe() + "."));
    }
}
