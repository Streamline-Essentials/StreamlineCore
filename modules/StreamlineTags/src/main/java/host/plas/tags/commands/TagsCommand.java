package host.plas.tags.commands;

import host.plas.tags.StreamlineTags;
import host.plas.tags.gui.TagsMenu;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.players.CosmicPlayer;
import singularity.gui.GuiManager;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@code /tags}: opens the menu for picking the active chat tag.
 */
public class TagsCommand extends ModuleCommand {
    public TagsCommand() {
        super(StreamlineTags.getInstance(), "tags", "streamline.command.tags.default", "chattags");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        if (context.getSender() == null || context.getSender().isConsole()) {
            context.sendMessage(StreamlineTags.message("players-only"));
            return;
        }
        if (! GuiManager.isAvailable()) {
            context.sendMessage(StreamlineTags.message("no-gui"));
            return;
        }
        Optional<CosmicPlayer> viewer = UserUtils.getOrCreatePlayer(context.getSender());
        viewer.ifPresent(player -> new TagsMenu().open(player));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        return new ConcurrentSkipListSet<>();
    }
}
