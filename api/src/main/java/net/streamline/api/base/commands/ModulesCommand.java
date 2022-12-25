package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
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
    public void run(StreamlineUser sender, String[] args) {
        if (args.length < 1) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reapply":
                if (args.length == 1) {
                    ModuleManager.getLoadedModules().forEach((s, module) -> ModuleManager.unregisterModule(module));
                    ModuleManager.registerExternalModules();
                    SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultReapplyAll);
                } else {
                    Arrays.stream(MessageUtils.argsMinus(args, 0)).forEach(a -> {
                        ModuleLike module = ModuleManager.getModule(a);
                        ModuleManager.reapplyModule(module.getIdentifier());
                        SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultReapplyOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
                break;
            case "reload":
                if (args.length == 1) {
                    ModuleManager.restartModules();
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
                break;
            case "load":
                if (args.length == 1) {
                    ModuleManager.registerExternalModules();
                    SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultLoadAll);
                } else {
                    Arrays.stream(MessageUtils.argsMinus(args, 0)).forEach(a -> {
                        if (ModuleManager.hasModule(a)) return;
                        ModuleManager.registerExternalModule(a);
                        SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultLoadOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
                break;
            case "unload":
                if (args.length == 1) {
                    ModuleManager.getLoadedModules().forEach(ModuleManager::unregisterModule);
                    SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultUnloadAll);
                } else {
                    Arrays.stream(MessageUtils.argsMinus(args, 0)).forEach(a -> {
                        if (! ModuleManager.hasModule(a)) return;
                        ModuleManager.unregisterModule(ModuleManager.getModule(a));
                        SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultUnloadOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
                break;
            case "enable":
                if (args.length == 1) {
                    ModuleManager.getLoadedModules().forEach((s, module) -> module.start());
                    SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultEnableAll);
                } else {
                    Arrays.stream(MessageUtils.argsMinus(args, 0)).forEach(a -> {
                        if (! ModuleManager.hasModule(a)) return;
                        ModuleManager.getModule(a).start();
                        SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultEnableOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
                break;
            case "disable":
                if (args.length == 1) {
                    ModuleManager.getLoadedModules().forEach((s, module) -> module.stop());
                    SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultDisableAll);
                } else {
                    Arrays.stream(MessageUtils.argsMinus(args, 0)).forEach(a -> {
                        if (! ModuleManager.hasModule(a)) return;
                        ModuleManager.getModule(a).stop();
                        SLAPI.getInstance().getMessenger().sendMessage(sender, messageResultDisableOne
                                .replace("%this_identifier%", a)
                        );
                    });
                }
                break;
            default:
                ModuleUtils.sendMessage(sender, getWithOther(sender, messageResultListAll, sender));
                break;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(StreamlineUser sender, String[] args) {
        if (args.length <= 1) {
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
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("reapply") || args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("unload")
                    || args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")) {
                return ModuleManager.getOnlyMalleableModuleIdentifiers();
            }
            if (args[0].equalsIgnoreCase("load")) {
                return ModuleManager.getUnloadedExternalModuleIdentifiers();
            }
        }
        return new ConcurrentSkipListSet<>();
    }
}
