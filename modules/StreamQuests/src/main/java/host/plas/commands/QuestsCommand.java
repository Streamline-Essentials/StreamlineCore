package host.plas.commands;

import host.plas.StreamQuests;
import host.plas.gui.QuestsMenu;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.gui.GuiManager;
import singularity.utils.UserUtils;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@code /quests}: opens a menu of every quest with the sender's progress on it.
 */
public class QuestsCommand extends ModuleCommand {
    public QuestsCommand() {
        super(StreamQuests.getInstance(), "quests", "streamline.command.quests.default", "quest", "qu");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        CosmicSender sender = context.getSender();
        if (sender == null || sender.isConsole()) {
            context.sendMessage("&cOnly players can view quests.");
            return;
        }
        if (! GuiManager.isAvailable()) {
            context.sendMessage("&cMenus are not available on this server.");
            return;
        }

        CosmicPlayer viewer = UserUtils.getOrCreatePlayer(sender).orElse(null);
        if (viewer == null) {
            context.sendMessage("&cFailed to load your quests.");
            return;
        }

        StreamQuests.getLoader().getOrCreateAsync(viewer.getUuid())
                .thenAccept(progress -> new QuestsMenu(progress).open(viewer))
                .exceptionally(error -> {
                    StreamQuests.getInstance().logWarning("Could not open quests for " + viewer.getCurrentName() + ": " + error);
                    context.sendMessage("&cFailed to load your quests.");
                    return null;
                });
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        return new ConcurrentSkipListSet<>();
    }
}
