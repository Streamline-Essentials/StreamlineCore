package gg.drak.tacoessentials.data;

import gg.drak.tacoessentials.commands.Msg;
import gg.drak.tacoessentials.teleport.Teleports;
import singularity.data.players.CosmicPlayer;
import singularity.utils.MessageUtils;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/**
 * What staff did to a player while they were offline (a teleport, a heal, a game mode change),
 * stored in the database and applied when the player next joins this server. Flight, god mode
 * and freezing need no queue: they are saved per player and applied at every login.
 */
public final class PendingActions {

    public static final String TELEPORT = "teleport";
    public static final String HEAL = "heal";
    public static final String FEED = "feed";
    public static final String GAMEMODE = "gamemode";

    private PendingActions() {}

    public static void teleport(String uuid, Loc to) {
        TacoDatabase.queue(uuid, TELEPORT, to.serialize());
    }

    public static void heal(String uuid) {
        TacoDatabase.queue(uuid, HEAL, null);
    }

    public static void feed(String uuid) {
        TacoDatabase.queue(uuid, FEED, null);
    }

    public static void gameMode(String uuid, String mode) {
        TacoDatabase.queue(uuid, GAMEMODE, mode);
    }

    /** Applies and clears everything queued for the player, oldest first. */
    public static void apply(CosmicPlayer player) {
        String uuid = player.getUuid();
        for (TacoDatabase.Pending pending : TacoDatabase.takePending(uuid)) {
            switch (pending.getAction()) {
                case TELEPORT:
                    Loc to = Loc.parse(pending.getData());
                    if (to != null && Teleports.teleport(uuid, to, true)) {
                        player.sendMessage(Msg.info("Staff moved you to " + to.describe() + " while you were away."));
                    }
                    break;
                case HEAL:
                    if (gameplay().heal(uuid)) player.sendMessage(Msg.success("You have been healed."));
                    break;
                case FEED:
                    if (gameplay().feed(uuid)) player.sendMessage(Msg.success("You have been fed."));
                    break;
                case GAMEMODE:
                    if (pending.getData() != null && gameplay().setGameMode(uuid, pending.getData())) {
                        player.sendMessage(Msg.success("Game mode set to " + pending.getData() + "."));
                    }
                    break;
                default:
                    MessageUtils.logWarning("[TacoEssentials] Skipped an unknown queued action '" + pending.getAction()
                            + "' for " + player.getCurrentName() + ".");
            }
        }
    }
}
