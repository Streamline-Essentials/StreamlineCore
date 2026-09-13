package host.plas.commands;

import singularity.command.ModuleCommand;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleLike;

import java.io.File;

public abstract class UtilitiesCMDBase extends ModuleCommand {
    public UtilitiesCMDBase(ModuleLike module, String base, String permission, String... aliases) {
        super(module, base, permission, aliases);
    }

    public UtilitiesCMDBase(ModuleLike module, String base, String permission, File parentDirectory, String... aliases) {
        super(module, base, permission, parentDirectory, aliases);
    }

    public enum SenderWithOther {
        FROM_IS_SENDER,
        TO_IS_SENDER,
    }

    public String getWithOther(SenderWithOther type, CosmicSender from, CosmicSender to, String message) {
        message = message
                .replace("%this_from%", from.getCurrentName())
                .replace("%this_to%", to.getCurrentName());
        CosmicSender sendTo = (type == SenderWithOther.FROM_IS_SENDER) ? from : to;
        CosmicSender other = (type == SenderWithOther.FROM_IS_SENDER) ? to : from;

        return getWithOther(sendTo, message, other);
    }
}
