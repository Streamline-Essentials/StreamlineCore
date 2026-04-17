package net.streamline.api.base.commands;

import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleLike;
import singularity.modules.ModuleManager;
import singularity.utils.MessageUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Command for managing the lifecycle of PF4J modules at runtime.
 *
 * <p>Supports the following sub-commands:
 * <ul>
 *   <li>{@code reapply [modules...]} — unregisters and re-registers modules.</li>
 *   <li>{@code reload [modules...]} — restarts modules.</li>
 *   <li>{@code load [modules...]} — registers previously unloaded modules.</li>
 *   <li>{@code unload [modules...]} — unregisters running modules.</li>
 *   <li>{@code enable [modules...]} — starts disabled modules.</li>
 *   <li>{@code disable [modules...]} — stops running modules.</li>
 * </ul>
 * When no module identifiers are provided the operation applies to all modules.
 * Registered under {@code streamlinemodules}, {@code module}, {@code modules},
 * {@code pmodules}, and {@code slm}.
 */
public class ModulesCommand extends CosmicCommand {

    /** Feedback message sent when all modules are re-applied successfully. */
    private final String messageResultReapplyAll;

    /** Feedback message sent when a single named module is re-applied. */
    private final String messageResultReapplyOne;

    /** Feedback message sent when all modules are reloaded successfully. */
    private final String messageResultReloadAll;

    /** Feedback message sent when a single named module is reloaded. */
    private final String messageResultReloadOne;

    /** Feedback message sent when all external modules are loaded. */
    private final String messageResultLoadAll;

    /** Feedback message sent when a single named module is loaded. */
    private final String messageResultLoadOne;

    /** Feedback message sent when all modules are unloaded. */
    private final String messageResultUnloadAll;

    /** Feedback message sent when a single named module is unloaded. */
    private final String messageResultUnloadOne;

    /** Feedback message sent when all modules are enabled. */
    private final String messageResultEnableAll;

    /** Feedback message sent when a single named module is enabled. */
    private final String messageResultEnableOne;

    /** Feedback message sent when all modules are disabled. */
    private final String messageResultDisableAll;

    /** Feedback message sent when a single named module is disabled. */
    private final String messageResultDisableOne;

    /** Feedback message listing all currently loaded modules. */
    private final String messageResultListAll;

    /**
     * Registers the modules command with the {@code streamline-base} module and
     * loads all configurable response messages from the command resource file,
     * falling back to built-in defaults when no configuration entry exists.
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Routes execution to the appropriate module lifecycle action.  When a
     * list of module identifiers follows the action keyword, only those modules
     * are affected; otherwise the action applies to every loaded module.</p>
     *
     * @param context the command context carrying the sender and parsed arguments
     */
    @Override
    public void run(CommandContext<CosmicCommand> context) {
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

    /**
     * {@inheritDoc}
     *
     * <p>Offers the available action keywords at argument position 1.  At
     * position 2, returns the identifiers of loaded (malleable) modules for
     * actions that target existing modules, or unloaded external module
     * identifiers for the {@code load} action.</p>
     *
     * @param context the command context carrying the sender and current argument list
     * @return a sorted set of tab-completion candidates
     */
    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
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
