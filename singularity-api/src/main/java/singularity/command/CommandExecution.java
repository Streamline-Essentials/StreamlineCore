package singularity.command;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a deferred command execution that captures both the sender identity
 * and the command string. Execution can be dispatched locally ({@link #executeHere}),
 * routed to a specific server ({@link #executeServer}), or resolved automatically
 * based on a server name ({@link #execute}).
 *
 * <p>The sender value supports several formats:</p>
 * <ul>
 *   <li>The console discriminator string → executes as console.</li>
 *   <li>{@code @c} → executes as console.</li>
 *   <li>{@code @n:&lt;name&gt;} → look up sender by name.</li>
 *   <li>{@code @u:&lt;uuid&gt;} → look up sender by UUID.</li>
 *   <li>{@code @&lt;name&gt;} → look up sender by name (shorthand).</li>
 *   <li>Plain string → look up sender by name.</li>
 * </ul>
 */
@Getter @Setter
public class CommandExecution {

    /**
     * The encoded representation of the sender (console discriminator, player name, or
     * {@code @}-prefixed reference).
     */
    private String senderValue;

    /** The command string to execute, without a leading slash. */
    private String command;

    /**
     * Creates a new {@code CommandExecution} with the given sender value and command.
     *
     * @param senderValue the encoded sender identifier
     * @param command     the command string to execute
     */
    public CommandExecution(String senderValue, String command) {
        setSenderValue(senderValue);
        setCommand(command);
    }

    /**
     * Resolves the {@link CosmicSender} from {@link #senderValue}.
     * Returns an empty {@link Optional} if the sender value is {@code null} or cannot
     * be resolved.
     *
     * @return an {@link Optional} containing the resolved sender, or empty if unresolvable
     */
    public Optional<CosmicSender> getSender() {
        if (senderValue == null) return Optional.empty();
        if (senderValue.equals(GivenConfigs.getMainConfig().getConsoleDiscriminator())) return Optional.of(UserUtils.getConsole());
        if (senderValue.startsWith("@")) {
            String thing = senderValue.substring(1);
            String[] split = thing.split(":", 2);

            if (split.length == 2) {
                String classifier = split[0];
                if (classifier.equals("n")) {
                    String name = split[1];
                    return UserUtils.getOrCreateSenderByName(name);
                } else if (classifier.equals("u")) {
                    return UserUtils.getOrCreateSender(split[1]);
                } else {
                    return UserUtils.getOrCreateSenderByName(split[1]);
                }
            } else {
                if (split[0].equals("c")) return Optional.of(UserUtils.getConsole());
                return UserUtils.getOrCreateSenderByName(split[0]);
            }
        }

        return UserUtils.getOrCreateSenderByName(senderValue);
    }

    /**
     * Dispatches the command either locally or to a remote server depending on
     * {@code serverInput}. The command runs locally when {@code serverInput} is
     * {@code null}, {@code "HERE"}, {@code "PROXY"}, {@code "--null"}, or matches
     * the current server name.
     *
     * @param serverInput the target server name, or one of the special tokens described above
     */
    public void execute(String serverInput) {
        if (serverInput == null) {
            executeHere();
        } else {
            if (
                    serverInput.equals("HERE") ||
                            (Singularity.isProxy() && (serverInput.equals("PROXY") || serverInput.equals("--null")) || serverInput.equals(GivenConfigs.getServerName()))
            ) {
                executeHere();
            } else {
                executeServer(serverInput);
            }
        }
    }

    /**
     * Executes the command locally without any server routing. Any failures are
     * handled silently (errors are not propagated to the caller).
     */
    public void executeMayFail() {
        executeHere();
    }

    /**
     * Resolves the sender and runs the command on the current server/proxy.
     * If the sender resolves to console the command is dispatched via the console;
     * otherwise it is dispatched via the sender's own {@code runCommand} method.
     */
    public void executeHere() {
        Optional<CosmicSender> optional = getSender();
        if (optional.isEmpty()) return;
        CosmicSender s = optional.get();
        if (s.isConsole()) {
            MessageUtils.logDebug("Executing command " + getCommand() + " as " + getSenderValue() + " on console.");
            ModuleUtils.getConsole().runCommand(getCommand());
        } else {
            MessageUtils.logDebug("Executing command " + getCommand() + " as " + getSenderValue() + " on " + s.getCurrentName() + ".");
            s.runCommand(getCommand());
        }
    }

    /**
     * Routes the command to a specific server by finding a carrier player on that
     * server and sending a {@link CommandMessageBuilder proxied command message}.
     * On a proxy the first player on {@code server} is used as the carrier; on a
     * backend server any online player is used instead.
     *
     * @param server the name of the target server to route the command to
     */
    public void executeServer(String server) {
        CosmicPlayer player = null;
        if (Singularity.isProxy()) {
            try {
                player = UserUtils.getPlayersOn(server).first();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            try {
                AtomicReference<CosmicPlayer> ref = new AtomicReference<>();
                UserUtils.getOnlinePlayers().forEach((s, p) -> {
                    if (ref.get() != null) return;

                    if (p.isOnline()) ref.set(p);
                });

                player = ref.get();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        if (player == null) {
            MessageUtils.logDebug("No player found on server " + server + " to execute command " + getCommand() + " as " + getSenderValue() + ".");
            return;
        }
        CommandMessageBuilder.build(player, server, this).send();
    }
}
