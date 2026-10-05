package singularity.events.player;

import lombok.Getter;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.CosmicSenderEvent;

/**
 * Fired on backend servers when a player runs any command, vanilla or modded, before it
 * executes. Cancelling it ({@link #setCancelled(boolean)}) stops the command from running.
 *
 * <p>Fabric has no hook for this, so the event is not fired there.</p>
 */
@Getter
public class CosmicCommandPreprocessEvent extends CosmicSenderEvent {

    /** The command as typed, without the leading slash. */
    private final String commandLine;

    /**
     * @param player      the player running the command
     * @param commandLine the command as typed; a leading slash is removed
     */
    public CosmicCommandPreprocessEvent(CosmicPlayer player, String commandLine) {
        super(player);
        this.commandLine = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
    }

    /**
     * @return the player running the command
     */
    public CosmicPlayer getPlayer() {
        return (CosmicPlayer) getSender();
    }

    /**
     * @return the command's first word, lower-cased, without any {@code namespace:} prefix
     */
    public String getLabel() {
        String label = commandLine.split(" ", 2)[0].toLowerCase();
        int colon = label.indexOf(':');
        return colon >= 0 ? label.substring(colon + 1) : label;
    }
}
