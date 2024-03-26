package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.command.context.CommandContext;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.modules.ModuleLike;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.utils.MessageUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentSkipListSet;

public class ModulesCommand extends StreamlineCommand {
    private final String messageResultReapplyAll;
    private final String messageResultReapplyOne;
    private final String messageResultReloadAll;
    private final String messageResultReloadOne;
    private final String messageResultLoadAll;
    private final String messageResultLoadOne;
    private final String messageResultUnloadAll;
    private final String messageResultUnloadOne;
    private final String messageResultEnableAll;
    private final String messageResultEnableOne;
    private final String messageResultDisableAll;
    private final String messageResultDisableOne;
    private final String messageResultListAll;

    public ModulesCommand() {
        super(
                "streamline-base",
                "streamlinemodules",
                "streamline.command.streamlinemodules.default",
                "module", "modules", "pmodules", "slm"
        );

        this.messageResultReapplyAll = this.getCommandResource().getOrSetDefault("messages.result.reapply.all",
                "&eRe-applied all modules&8!");
        this.messageResultReloadAll = this.getCommandResource().getOrSetDefault("messages.result.reload.all",
                "&eReloaded all modules&8!");
        this.messageResultLoadAll = this.getCommandResource().getOrSetDefault("messages.result.load.all",
                "&eLoaded all modules&8!");
        this.messageResultUnloadAll = this.getCommandResource().getOrSetDefault("messages.result.unload.all",
                "&eUnloaded all modules&8!");
        this.messageResultEnableAll = this.getCommandResource().getOrSetDefault("messages.result.enable.all",
                "&eEnabled all modules&8!");
        this.messageResultDisableAll = this.getCommandResource().getOrSetDefault("messages.result.disable.all",
                "&eDisabled all modules&8!");
        this.messageResultListAll = this.getCommandResource().getOrSetDefault("messages.result.list.all",
                "&eModules: &8%streamline_modules_colorized%&8!");

        this.messageResultReapplyOne = this.getCommandResource().getOrSetDefault("messages.result.reapply.one",
                "&eRe-applied module &7'&c%this_identifier%&7'&8!");
        this.messageResultReloadOne = this.getCommandResource().getOrSetDefault("messages.result.reload.one",
                "&eReloaded module &7'&c%this_identifier%&7'&8!");
        this.messageResultLoadOne = this.getCommandResource().getOrSetDefault("messages.result.load.one",
                "&eLoaded module &7'&c%this_identifier%&7'&8!");
        this.messageResultUnloadOne = this.getCommandResource().getOrSetDefault("messages.result.unload.one",
                "&eUnloaded module &7'&c%this_identifier%&7'&8!");
        this.messageResultEnableOne = this.getCommandResource().getOrSetDefault("messages.result.enable.one",
                "&eEnabled module &7'&c%this_identifier%&7'&8!");
        this.messageResultDisableOne = this.getCommandResource().getOrSetDefault("messages.result.disable.one",
                "&eDisabled module &7'&c%this_identifier%&7'&8!");
    }

    @Override
    public void run(CommandContext<StreamlineCommand> context) {
        if (context.getArgCount() < 1) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        switch (context.getStringArg(0).toLowerCase(Locale.ROOT)) {
            case "reapply":
                if (context.getArgCount() == 1) {
                    ModuleManager.getLoadedModules().forEach((s, module) -> ModuleManager.unregisterModule(module));
                    ModuleManager.registerExternalModules();
                    context.sendMessage(messageResultReapplyAll);
                } else {
                    Arrays.stream(MessageUtils.argsMinus(context.getArgsArray(), 0)).forEach(a -> {
                        ModuleLike module = ModuleManager.getModule(a);
                        ModuleManager.reapplyModule(module.getIdentifier());
                        context.sendMessage(messageResultReapplyOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
                break;
            case "reload":
                if (context.getArgCount() == 1) {
                    ModuleManager.restartModules();
                    context.sendMessage(messageResultReloadAll);
                } else {
                    Arrays.stream(MessageUtils.argsMinus(context.getArgsArray(), 0)).forEach(a -> {
                        if (! ModuleManager.hasModule(a)) return;
                        ModuleManager.getModule(a).restart();
                        context.sendMessage(messageResultReloadOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
                break;
            case "load":
                if (context.getArgCount() == 1) {
                    ModuleManager.registerExternalModules();
                    context.sendMessage(messageResultLoadAll);
                } else {
                    Arrays.stream(MessageUtils.argsMinus(context.getArgsArray(), 0)).forEach(a -> {
                        if (ModuleManager.hasModule(a)) return;
                        ModuleManager.registerExternalModule(a);
                        context.sendMessage(messageResultLoadOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
                break;
            case "unload":
                if (context.getArgCount() == 1) {
                    ModuleManager.getLoadedModules().forEach(ModuleManager::unregisterModule);
                    context.sendMessage(messageResultUnloadAll);
                } else {
                    Arrays.stream(MessageUtils.argsMinus(context.getArgsArray(), 0)).forEach(a -> {
                        if (! ModuleManager.hasModule(a)) return;
                        ModuleManager.unregisterModule(ModuleManager.getModule(a));
                        context.sendMessage(messageResultUnloadOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
                break;
            case "enable":
                if (context.getArgCount() == 1) {
                    ModuleManager.getLoadedModules().forEach((s, module) -> module.start());
                    context.sendMessage(messageResultEnableAll);
                } else {
                    Arrays.stream(MessageUtils.argsMinus(context.getArgsArray(), 0)).forEach(a -> {
                        if (! ModuleManager.hasModule(a)) return;
                        ModuleManager.getModule(a).start();
                        context.sendMessage(messageResultEnableOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
                break;
            case "disable":
                if (context.getArgCount() == 1) {
                    ModuleManager.getLoadedModules().forEach((s, module) -> module.stop());
                    context.sendMessage(messageResultDisableAll);
                } else {
                    Arrays.stream(MessageUtils.argsMinus(context.getArgsArray(), 0)).forEach(a -> {
                        if (! ModuleManager.hasModule(a)) return;
                        ModuleManager.getModule(a).stop();
                        context.sendMessage(messageResultDisableOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
                break;
            default:
                context.sendMessage(getWithOther(context.getSender(), messageResultListAll, context.getSender()));
                break;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<StreamlineCommand> context) {
        if (context.getArgCount() <= 1) {
            return new ConcurrentSkipListSet<>(List.of(
                    "reapply",
                    "reload",
                    "load",
                    "unload",
                    "enable",
                    "disable",
                    "list"
            ));
        }
        if (context.getArgCount() == 2) {
            if (context.getStringArg(0).equalsIgnoreCase("reapply") || context.getStringArg(0).equalsIgnoreCase("reload")
                    || context.getStringArg(0).equalsIgnoreCase("unload") || context.getStringArg(0).equalsIgnoreCase("enable")
                    || context.getStringArg(0).equalsIgnoreCase("disable")) {
                return ModuleManager.getOnlyMalleableModuleIdentifiers();
            }
            if (context.getStringArg(0).equalsIgnoreCase("load")) {
                return ModuleManager.getUnloadedExternalModuleIdentifiers();
            }
        }
        return new ConcurrentSkipListSet<>();
    }
}
