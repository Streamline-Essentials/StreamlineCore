package host.plas.commands;

import host.plas.StreamQuests;
import host.plas.data.QuestManager;
import singularity.command.ModuleCommand;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.utils.UserUtils;

import java.util.concurrent.CompletableFuture;

public class ReloadCommand extends ModuleCommand {
    public ReloadCommand() {
        super(StreamQuests.getInstance(), "reloadquests", "streamline.command.reloadquests.default", "relquests", "rlq");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        context.sendMessage("&eSaving players&8...");

        CompletableFuture.runAsync(() -> {
            StreamQuests.getLoader().getLoaded().forEach(player -> {
                StreamQuests.getKeeper().save(player, false);
            });

            context.sendMessage("&eSaved &a" + StreamQuests.getLoader().getLoaded().size() + " &eplayers&8.");

            StreamQuests.getLoader().getLoaded().clear(); // might want to disable this later...

            UserUtils.getOnlinePlayers().forEach((uuid, player) -> {
                StreamQuests.getLoader().getOrCreate(uuid);
            });

            context.sendMessage("&eLoaded &a" + StreamQuests.getLoader().getLoaded().size() + " &eplayers&8.");
        });

        context.sendMessage("&eReloading &a" + QuestManager.getQuests().size() + " &equests&8...");

        StreamQuests.getExampleConfig().reloadConfig();

        context.sendMessage("&aLoaded &e" + QuestManager.getQuests().size() + " &aquests&8.");
    }
}
