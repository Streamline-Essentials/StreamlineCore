package gg.drak.tacoessentials.activity;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.commands.Msg;
import gg.drak.tacoessentials.data.Loc;
import gg.drak.tacoessentials.data.Sessions;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.modules.ModuleUtils;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.Optional;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/**
 * AFK status. {@code /afk} toggles it; a player who has not moved, turned, chatted or run a
 * command for {@code afk.idle-seconds} becomes AFK, and any of those brings them back. Going AFK
 * and coming back are announced when {@code afk.broadcast} is on. Kept in memory only.
 *
 * <p>Movement is noticed by {@link #tick()} comparing positions, since not every platform fires
 * a movement event.</p>
 */
public final class Afk {

    private Afk() {}

    public static boolean isAfk(String uuid) {
        return Sessions.online().contains(uuid) && Sessions.get(uuid).isAfk();
    }

    /** The player did something; ends their AFK status. */
    public static void active(String uuid) {
        if (! Sessions.online().contains(uuid)) return;
        Sessions.Session session = Sessions.get(uuid);
        session.markActive();
        if (session.isAfk()) set(uuid, false);
    }

    /** Sets the player's AFK status and announces the change. */
    public static void set(String uuid, boolean afk) {
        Sessions.Session session = Sessions.get(uuid);
        if (session.isAfk() == afk) return;
        session.setAfk(afk);
        if (! afk) session.markActive();
        // From here on only moving away from where they are now counts as coming back.
        if (afk) gameplay().getLocation(uuid).ifPresent(here -> session.setLastSeen(Loc.of(here)));
        Optional<CosmicPlayer> player = UserUtils.getPlayer(uuid).filter(CosmicPlayer::isOnline);
        if (player.isEmpty()) return;
        String name = player.get().getCurrentName();
        if (TacoEssentials.getConfig().afkBroadcast()) {
            String line = Msg.muted("* " + name + (afk ? " is now AFK." : " is no longer AFK."));
            Singularity.getInstance().getPlatform().getOnlinePlayers().forEach(p -> p.sendMessage(line));
            MessageUtils.logInfo(ModuleUtils.stripColor(line));
        } else {
            player.get().sendMessage(Msg.muted(afk ? "You are now AFK." : "You are no longer AFK."));
        }
    }

    /** Notices players who moved or turned, and marks idle ones AFK; every five seconds. */
    public static void tick() {
        int idleSeconds = TacoEssentials.getConfig().afkIdleSeconds();
        long now = System.currentTimeMillis();
        for (String uuid : Sessions.online()) {
            Optional<CosmicLocation> here = gameplay().getLocation(uuid);
            if (here.isEmpty()) continue;
            Sessions.Session session = Sessions.get(uuid);
            Loc loc = Loc.of(here.get());
            Loc last = session.getLastSeen();
            session.setLastSeen(loc);
            if (last != null && ! samePose(last, loc)) {
                active(uuid);
                continue;
            }
            if (idleSeconds > 0 && ! session.isAfk() && now - session.getLastActivityMillis() >= idleSeconds * 1000L) {
                set(uuid, true);
            }
        }
    }

    private static boolean samePose(Loc a, Loc b) {
        return a.getWorld().equals(b.getWorld()) && a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ()
                && a.getYaw() == b.getYaw() && a.getPitch() == b.getPitch();
    }
}
