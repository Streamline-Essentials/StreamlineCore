package host.plas.stonedamager.commands;

import host.plas.stonedamager.StoneDamager;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * {@code /reloadstonedamager} — re-reads config.yml.
 */
public class ReloadCommand extends ModuleCommand {
    public ReloadCommand() {
        super(StoneDamager.getInstance(), "reloadstonedamager", "streamline.command.reloadstonedamager.default",
                "rlsd", "reloadsd");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        StoneDamager.getDamagerConfig().init();
        context.sendMessage("&eReloaded &cconfigurations&8: &a"
                + StoneDamager.getDamagerConfig().getSelections().size() + " &edamager selections&8.");
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        return new ConcurrentSkipListSet<>();
    }
}
