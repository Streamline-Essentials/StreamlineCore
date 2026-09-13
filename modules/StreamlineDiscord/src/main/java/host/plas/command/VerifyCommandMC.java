package host.plas.command;

import host.plas.StreamlineDiscord;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;

import java.util.concurrent.ConcurrentSkipListSet;

public class VerifyCommandMC extends ModuleCommand {
    String messageUnverified;
    String messageVerified;

    public VerifyCommandMC() {
        super(StreamlineDiscord.getInstance(),
                "verify",
                "streamline.command.verify.default",
                "dv"
        );

        messageUnverified = getCommandResource().getOrSetDefault("messages.reply.unverified",
                "&eVerify your &9&lDiscord &eby private messaging &d%discord_bot_name_tagged% &eon &9&lDiscord &ethis code&8: &a%discord_user_verification_code%");
        messageVerified = getCommandResource().getOrSetDefault("messages.reply.verified",
                "&eYour &9&lDiscord &eis &calready &averified&8! &eYou are verified with this &9&lDiscord &eaccount&8: &d%discord_user_name_tagged%");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        CosmicSender streamlineUser = context.getSender();

        if (StreamlineDiscord.getVerifiedUsers().isVerified(streamlineUser)) {
            ModuleUtils.sendMessage(streamlineUser, messageVerified);
        } else {
            ModuleUtils.sendMessage(streamlineUser, messageUnverified);
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        return new ConcurrentSkipListSet<>();
    }
}
