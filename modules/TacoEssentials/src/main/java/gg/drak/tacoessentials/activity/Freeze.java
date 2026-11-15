package gg.drak.tacoessentials.activity;

import gg.drak.tacoessentials.data.Loc;
import gg.drak.tacoessentials.data.Sessions;
import gg.drak.tacoessentials.teleport.Teleports;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import singularity.data.players.location.CosmicLocation;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/**
 * Frozen players ({@code /freeze}) are held in the block they stood in when frozen, or joined in,
 * and may only run the private-message commands. Freezing is saved per player, so it lasts across
 * relogs until staff unfreeze them.
 *
 * <p>Not every platform fires a movement event, so {@link #tick()} pulls players back twice a
 * second; a frozen player can move a little between checks.</p>
 */
public final class Freeze {

    /** Commands a frozen player may still run, so they can talk to staff. */
    public static final Set<String> ALLOWED_COMMANDS = new HashSet<>(Arrays.asList(
            "msg", "tell", "w", "whisper", "pm", "dm", "r", "reply"));

    private Freeze() {}

    public static void tick() {
        for (String uuid : Sessions.online()) {
            if (! Sessions.isFrozen(uuid)) continue;
            Sessions.Session session = Sessions.get(uuid);
            Optional<CosmicLocation> here = gameplay().getLocation(uuid);
            if (here.isEmpty()) continue;
            Loc at = session.getFrozenAt();
            if (at == null) {
                session.setFrozenAt(Loc.of(here.get()));
                continue;
            }
            Loc now = Loc.of(here.get());
            if (now.sameBlock(at)) continue;
            Teleports.teleport(uuid, new Loc(at.getWorld(), at.getX(), at.getY(), at.getZ(), now.getYaw(), now.getPitch()), false);
        }
    }
}
