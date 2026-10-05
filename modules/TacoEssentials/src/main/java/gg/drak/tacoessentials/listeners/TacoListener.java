package gg.drak.tacoessentials.listeners;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.commands.Msg;
import gg.drak.tacoessentials.data.Loc;
import gg.drak.tacoessentials.data.Sessions;
import gg.drak.tacoessentials.data.TacoDatabase;
import gg.drak.tacoessentials.teleport.Teleports;
import gg.drak.tacoessentials.teleport.TpaManager;
import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import singularity.data.players.CosmicPlayer;
import singularity.events.player.CosmicCommandPreprocessEvent;
import singularity.events.player.CosmicDeathEvent;
import singularity.events.server.CosmicChatEvent;
import singularity.events.server.LoginCompletedEvent;
import singularity.events.server.LogoutEvent;
import singularity.utils.MessageUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

public class TacoListener implements BaseEventListener {

    /** Commands a muted player may not use, since each one broadcasts or delivers their text. */
    private static final Set<String> CHAT_COMMANDS = new HashSet<>(Arrays.asList(
            "msg", "tell", "w", "me", "say", "teammsg", "tm", "whisper", "r", "reply"));

    /** Vanilla teleport commands, whose starting point is recorded for /back. */
    private static final Set<String> TELEPORT_COMMANDS = new HashSet<>(Arrays.asList("tp", "teleport"));

    @BaseProcessor
    public void onLogin(LoginCompletedEvent event) {
        CosmicPlayer player = event.getPlayer();
        if (player == null) return;
        String uuid = player.getUuid();
        try {
            TacoDatabase.recordPlayer(uuid, player.getCurrentName());
            Sessions.Session session = Sessions.load(uuid);
            if (session.isFly()) gameplay().setFlight(uuid, true);
            sendToFirstSpawn(uuid);
        } catch (Exception e) {
            MessageUtils.logWarning("[TacoEssentials] Could not load data for " + player.getCurrentName() + ": " + e.getMessage());
        }
    }

    /**
     * New players go to the {@code /setfirstspawn} location. "New" comes from the game's own
     * records rather than this module's database, so players who joined before the module was
     * installed are not treated as newcomers.
     */
    private static void sendToFirstSpawn(String uuid) {
        if (gameplay().hasPlayedBefore(uuid)) return;
        TacoDatabase.serverLocation(TacoDatabase.FIRST_SPAWN).ifPresent(loc -> {
            if (! Teleports.teleport(uuid, loc, false)) {
                MessageUtils.logWarning("[TacoEssentials] The first-join spawn is in a missing world: " + loc.getWorld());
            }
        });
    }

    @BaseProcessor
    public void onLogout(LogoutEvent event) {
        CosmicPlayer player = event.getPlayer();
        if (player == null) return;
        String uuid = player.getUuid();
        TpaManager.dropPlayer(uuid);
        try {
            gameplay().getLocation(uuid).ifPresent(here -> TacoDatabase.saveLastLocation(uuid, Loc.of(here)));
        } catch (Exception e) {
            MessageUtils.logWarning("[TacoEssentials] Could not save the last location of " + player.getCurrentName() + ": " + e.getMessage());
        }
        Sessions.unload(uuid);
    }

    @BaseProcessor
    public void onChat(CosmicChatEvent event) {
        CosmicPlayer player = event.getPlayer();
        if (player == null || ! Sessions.isMuted(player.getUuid())) return;
        event.setCanceled(true);
        player.sendMessage(Msg.error("You are muted."));
    }

    @BaseProcessor
    public void onCommand(CosmicCommandPreprocessEvent event) {
        CosmicPlayer player = event.getPlayer();
        if (player == null) return;
        String label = event.getLabel();
        if (CHAT_COMMANDS.contains(label) && Sessions.isMuted(player.getUuid())) {
            event.setCancelled(true);
            player.sendMessage(Msg.error("You are muted."));
            return;
        }
        if (TELEPORT_COMMANDS.contains(label)) Teleports.recordBack(player.getUuid());
    }

    @BaseProcessor
    public void onDeath(CosmicDeathEvent event) {
        CosmicPlayer player = event.getPlayer();
        if (player == null) return;
        Loc loc = Loc.of(event.getLocation());
        try {
            TacoDatabase.push(player.getUuid(), TacoDatabase.DEATH, loc, TacoEssentials.getConfig().deathHistorySize());
            if (TacoEssentials.getConfig().backOnDeath()) Teleports.recordBack(player.getUuid(), loc);
        } catch (Exception e) {
            MessageUtils.logWarning("[TacoEssentials] Could not record the death of " + player.getCurrentName() + ": " + e.getMessage());
        }
        player.sendMessage(Msg.muted(TacoEssentials.getConfig().backOnDeath()
                ? "Use /back or /dback 1 to return to where you died."
                : "Use /dback 1 to return to where you died."));
    }
}
