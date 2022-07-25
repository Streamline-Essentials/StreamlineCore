package net.streamline.base.commands;

import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.utils.MessagingUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ModulesCommand extends StreamlineCommand {
    private final String messageResultReapplyAll;
    private final String messageResultReapplyOne;
    private final String messageResultReloadAll;
    private final String messageResultReloadOne;

    public ModulesCommand() {
        super(
                "streamlinemodule",
                "streamline.command.streamlinemodule.default",
                "module", "pmodule", "slm"
        );

        this.messageResultReapplyAll = this.getCommandResource().getOrSetDefault("messages.result.reapply.all",
                "&eRe-applied all modules&8!");
        this.messageResultReloadAll = this.getCommandResource().getOrSetDefault("messages.result.reload.all",
                "&eReloaded all modules&8!");

        this.messageResultReapplyOne = this.getCommandResource().getOrSetDefault("messages.result.reapply.one",
                "&eRe-applied module &7'&c%this_identifier%&7'&8!");
        this.messageResultReloadOne = this.getCommandResource().getOrSetDefault("messages.result.reload.one",
                "&eReloaded module &7'&c%this_identifier%&7'&8!");
    }

    @Override
    public void run(SavableUser sender, String[] args) {
        if (args.length < 1) {
            MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reapply" -> {
                if (args.length == 1) {
                    new ArrayList<>(ModuleManager.loadedModules.values()).forEach(ModuleManager::unregisterModule);
                    ModuleManager.registerExternalModules();
                    MessagingUtils.sendMessage(sender, messageResultReapplyAll);
                } else {
                    Arrays.stream(MessagingUtils.argsMinus(args, 0)).forEach(a -> {
                        StreamlineModule module = ModuleManager.getModule(a);
                        ModuleManager.reapplyModule(module);
                        MessagingUtils.sendMessage(sender, messageResultReapplyOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
            }
            case "reload" -> {
                if (args.length == 1) {
                    new ArrayList<>(ModuleManager.loadedModules.values()).forEach(StreamlineModule::restart);
                    MessagingUtils.sendMessage(sender, messageResultReloadAll);
                } else {
                    Arrays.stream(MessagingUtils.argsMinus(args, 0)).forEach(a -> {
                        ModuleManager.getModule(a).restart();
                        MessagingUtils.sendMessage(sender, messageResultReloadOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
            }
        }
    }

    @Override
    public List<String> doTabComplete(SavableUser sender, String[] args) {
        if (args.length <= 1) {
            return List.of(
                    "reapply",
                    "reload"
            );
        }
        if (args.length == 2) {
            return new ArrayList<>(ModuleManager.loadedModules.keySet());
        }
        return new ArrayList<>();
    }
}
