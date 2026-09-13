package host.plas.commands;

import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleUtils;
import singularity.data.players.CosmicPlayer;
import singularity.data.console.CosmicSender;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;
import host.plas.StreamlineUtilities;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
public class MaintenanceCommand extends ModuleCommand {
    private final String messageResultAll;
    private final String messageResultAdd;
    private final String messageResultRemove;

    public MaintenanceCommand() {
        super(StreamlineUtilities.getInstance(),
                "proxymaintenance",
                "streamline.command.maintenance.default",
                "pmaint", "pmaintenance", "pmnt"
        );

        messageResultAll = getCommandResource().getOrSetDefault("messages.result.all", "&cMaintenance Mode &eis now %utils_maintenance_mode% &b(&ewas %this_previous%&b)&8.");
        messageResultAdd = getCommandResource().getOrSetDefault("messages.result.add",
                "&eAdded &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &eto &cMaintenance Mode &ewhitelist&e.");
        messageResultRemove = getCommandResource().getOrSetDefault("messages.result.remove",
                "&eRemoved &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &efrom &cMaintenance Mode &ewhitelist&e.");
    }

    @Override
    public void run(CosmicSender CosmicSender, String[] strings) {
        if (strings.length < 1) {
            strings = new String[] { String.valueOf(! StreamlineUtilities.getMaintenanceConfig().isModeEnabled()) };
        }
//        if (strings.length > 1) {
//            ModuleUtils.sendMessage(CosmicSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_MANY.get());
//            return;
//        }

        if (strings.length == 1) {
            if (strings[0].equals("add") || strings[0].equals("remove")) {
                ModuleUtils.sendMessage(CosmicSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                return;
            }
            try {
                String previous = ModuleUtils.replacePlaceholders("%utils_maintenance_mode%");
                boolean bool = Boolean.parseBoolean(strings[0]);
                StreamlineUtilities.getMaintenanceConfig().setModeEnabled(bool);
                ModuleUtils.sendMessage(CosmicSender, getWithOther(CosmicSender, getMessageResultAll(), CosmicSender)
                        .replace("%this_previous%", previous)
                );

                if (bool) {
                    ModuleUtils.getLoadedPlayers().forEach((s, player) -> {
                        if (StreamlineUtilities.getMaintenanceConfig().containsAllowed(player.getUuid())) return;
                        ModuleUtils.kick(player, ModuleUtils.replaceAllPlayerBungee(CosmicSender, StreamlineUtilities.getMaintenanceConfig().getModeKickMessage()));
                    });
                }
                return;
            } catch (Exception e) {
                ModuleUtils.sendMessage(CosmicSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                return;
            }
        }

        switch (strings[0]) {
            case "add":
                ConcurrentSkipListSet<String> names = Arrays.stream(MessageUtils.argsMinus(strings, 0)).collect(Collectors.toCollection(ConcurrentSkipListSet::new));

                AtomicInteger atomicInteger = new AtomicInteger(0);
                names.forEach(s -> {
                    CosmicPlayer player = UserUtils.getOrCreatePlayerByName(s).orElse(null);
                    if (player == null) {
                        ModuleUtils.sendMessage(CosmicSender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_OTHER.get());
                        return;
                    }

                    StreamlineUtilities.getMaintenanceConfig().addAllowed(player.getUuid());
                    atomicInteger.getAndAdd(1);

                    ModuleUtils.sendMessage(CosmicSender,
                            getWithOther(CosmicSender, getMessageResultAdd(), player)
                                    .replace("%this_index%", String.valueOf(atomicInteger.get())
                                    )
                    );
                });

                StreamlineUtilities.getInstance().logInfo("Added " + atomicInteger.get() + " users to the &cMaintenance Mode &awhitelist&f!");
                break;
            case "remove":
                ConcurrentSkipListSet<String> namesRemove = Arrays.stream(MessageUtils.argsMinus(strings, 0)).collect(Collectors.toCollection(ConcurrentSkipListSet::new));

                AtomicInteger atomicIntegerRemove = new AtomicInteger(0);
                namesRemove.forEach(s -> {
                    CosmicPlayer player = UserUtils.getOrCreatePlayerByName(s).orElse(null);
                    if (player == null) {
                        ModuleUtils.sendMessage(CosmicSender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_OTHER.get());
                        return;
                    }

                    StreamlineUtilities.getMaintenanceConfig().removeAllowed(player.getUuid());

                    if (StreamlineUtilities.getMaintenanceConfig().isModeEnabled()) {
                        if (player.isOnline()) ModuleUtils.kick(player, "%utils_maintenance_message%");
                    }

                    atomicIntegerRemove.getAndAdd(1);

                    ModuleUtils.sendMessage(CosmicSender,
                            getWithOther(CosmicSender, getMessageResultRemove(), player)
                                    .replace("%this_index%", String.valueOf(atomicIntegerRemove.get())
                                    )
                    );
                });

                StreamlineUtilities.getInstance().logInfo("Removed " + atomicIntegerRemove.get() + " users from the &cMaintenance Mode &awhitelist&f!");
                break;
            default:
                ModuleUtils.sendMessage(CosmicSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                return;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        if (strings.length == 1) return new ConcurrentSkipListSet<>(List.of("true", "false", "add", "remove"));
        if (strings.length >= 2) {
            if (strings[0].equals("add") || strings[0].equals("remove")) return ModuleUtils.getOnlinePlayerNames();
        }

        return new ConcurrentSkipListSet<>();
    }
}
