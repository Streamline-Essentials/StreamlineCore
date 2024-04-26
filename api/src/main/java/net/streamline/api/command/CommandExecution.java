package net.streamline.api.command;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Getter @Setter
public class CommandExecution {
    private String senderValue;
    private String command;

    public CommandExecution(String senderValue, String command) {
        setSenderValue(senderValue);
        setCommand(command);
    }

    public Optional<StreamSender> getSender() {
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
                    return Optional.of(UserUtils.getOrCreateSender(split[1]));
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

    public void execute(String serverInput) {
        if (serverInput == null) {
            executeHere();
        } else {
            if (
                    serverInput.equals("HERE") ||
                    (SLAPI.isProxy() && (serverInput.equals("PROXY") || serverInput.equals("--null")))
            ) {
                executeHere();
            } else {
                executeServer(serverInput);
            }
        }
    }

    public void executeHere() {
        Optional<StreamSender> optional = getSender();
        if (optional.isEmpty()) return;
        StreamSender s = optional.get();
        if (s.isConsole()) {
            MessageUtils.logDebug("Executing command " + getCommand() + " as " + getSenderValue() + " on console.");
            ModuleUtils.getConsole().runCommand(getCommand());
        } else {
            MessageUtils.logDebug("Executing command " + getCommand() + " as " + getSenderValue() + " on " + s.getCurrentName() + ".");
            s.runCommand(getCommand());
        }
    }

    public void executeServer(String server) {
        StreamPlayer player = null;
        if (SLAPI.isProxy()) {
            try {
                player = UserUtils.getPlayersOn(server).first();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            try {
                AtomicReference<StreamPlayer> ref = new AtomicReference<>();
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
