package host.plas.commands;

import host.plas.StreamlineMessaging;
import host.plas.configs.ConfiguredChatChannel;
import host.plas.database.MyLoader;
import host.plas.savables.ChatterManager;
import host.plas.savables.SavableChatter;
import lombok.Getter;
import lombok.Setter;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.command.result.CommandResult;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class ChatterAdminCommand extends ModuleCommand {

    public ChatterAdminCommand() {
        super(StreamlineMessaging.getInstance(),
                "chatteradmin",
                "streamline.command.chatteradmin",
                "cadm", "adminchatter", "cadmin"
        );
    }

    @Override
    public CommandResult<?> resultedRun(CommandContext<CosmicCommand> ctx) {
        CosmicSender sender = ctx.getSender();

        try  {
            if (! ctx.isArgUsable(0)) {
                sender.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());

                return failure();
            }

            String option = ctx.getStringArg(0).toLowerCase();

            switch (option) {
                case "users":
                    if (! ctx.isArgUsable(1)) {
                        sender.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                        return failure();
                    }

                    String action = ctx.getStringArg(1);

                    switch (action) {
                        case "delete":
                            if (! ctx.isArgUsable(2)) {
                                sender.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                                return failure();
                            }

                            String user = ctx.getStringArg(2);
                            Optional<CosmicSender> optional = UserUtils.getOrGetSenderByName(user);
                            if (optional.isEmpty()) {
                                sender.sendMessage(MainMessagesHandler.MESSAGES.INVALID.PLAYER_OTHER.get());
                                return failure();
                            }
                            CosmicSender other = optional.get();
                            MyLoader.getInstance().get(other.getUuid()).ifPresent(chatter -> {
                                chatter.unload();
                                StreamlineMessaging.getKeeper().delete(other.getUuid());
                            });

                            return success();
                        default:
                            sender.sendMessage("&cThat is not an option.");
                            return failure();
                    }
                default:
                    sender.sendMessage("&cThat is not an option.");
                    return failure();
            }
        } catch (Exception e) {
            sender.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
            return error();
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        if (strings.length <= 1) {
            ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

            StreamlineMessaging.getChatChannelConfig().getChatChannels().forEach((a, b) -> {
                if (ModuleUtils.hasPermission(CosmicSender, b.getAccessPermission())) r.add(a);
            });

            return r;
        }

        return new ConcurrentSkipListSet<>();
    }
}
