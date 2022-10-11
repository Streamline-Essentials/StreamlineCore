package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentSkipListSet;

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
    public void run(StreamlineUser sender, String[] args) {
        if (args.length < 1) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reapply" -> {
                if (args.length == 1) {
                    ModuleManager.getLoadedModules().forEach((s, module) -> ModuleManager.unregisterModule(module));
                    ModuleManager.registerExternalModules();
                    SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultReapplyAll);
                } else {
                    Arrays.stream(MessageUtils.argsMinus(args, 0)).forEach(a -> {
                        ModuleLike module = ModuleManager.getModule(a);
                        ModuleManager.reapplyModule(module.identifier());
                        SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultReapplyOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
            }
            case "reload" -> {
                if (args.length == 1) {
                    ModuleManager.getLoadedModules().forEach((s, module) -> module.restart());
                    SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultReloadAll);
                } else {
                    Arrays.stream(MessageUtils.argsMinus(args, 0)).forEach(a -> {
                        if (! ModuleManager.hasModule(a)) return;
                        ModuleManager.getModule(a).restart();
                        SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultReloadOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
            }
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(StreamlineUser sender, String[] args) {
        if (args.length <= 1) {
            return new ConcurrentSkipListSet<>(List.of(
                    "reapply",
                    "reload"
            ));
        }
        if (args.length == 2) {
            return new ConcurrentSkipListSet<>(ModuleManager.getLoadedModules().keySet());
        }
        return new ConcurrentSkipListSet<>();
    }
}
