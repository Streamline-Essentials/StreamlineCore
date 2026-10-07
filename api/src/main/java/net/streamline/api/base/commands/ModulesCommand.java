package net.streamline.api.base.commands;

import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleCloud;
import singularity.objects.ClickableMessage;
import singularity.modules.ModuleLike;
import singularity.modules.ModuleManager;
import singularity.utils.MessageUtils;

import java.time.Duration;
import java.util.ArrayList;
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
 *   <li>{@code ecloud download <module> [version]} — downloads a module from the
 *       module registry into the module folder and loads it.</li>
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

    /** Sent when an eCloud download starts. */
    private final String messageEcloudDownloading;

    /** Sent when an eCloud download was saved and loaded. */
    private final String messageEcloudDownloaded;

    /** Sent when an eCloud download was saved but could not be loaded. */
    private final String messageEcloudDownloadedNotLoaded;

    /** Sent when an eCloud download fails or is refused. */
    private final String messageEcloudFailed;

    /** Usage hint for the {@code ecloud} sub-command. */
    private final String messageEcloudUsage;

    /** Usage hint for {@code ecloud get}. */
    private final String messageEcloudGetUsage;

    /** Sent while an {@code ecloud get} lookup runs. */
    private final String messageEcloudGetLooking;

    /** Sent when an {@code ecloud get} lookup fails. */
    private final String messageEcloudGetFailed;

    /** {@code ecloud get downloads} answer. */
    private final String messageEcloudGetDownloads;

    /** {@code ecloud get version} answer. */
    private final String messageEcloudGetVersion;

    /** {@code ecloud get all} answer, one entry per line; a {@code %this_versions%} line expands to the version lines. */
    private final List<String> messageEcloudGetAll;

    /** One line of {@code %this_versions%} in the {@code ecloud get all} answer. */
    private final String messageEcloudGetAllVersion;

    /** How many versions {@code ecloud get all} lists, newest first. */
    private final int ecloudGetAllVersions;

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

        ModuleCloud.setBaseUrl(this.getCommandResource().getOrSetDefault("ecloud.url", ModuleCloud.DEFAULT_BASE_URL));
        // Fetches right away, so the first "/modules ecloud download " already has names, then every interval.
        // An int default: YAML reads small numbers back as Integer, which a Long default would fail to cast.
        int refreshSeconds = this.getCommandResource().getOrSetDefault("ecloud.refresh-interval-seconds",
                (int) ModuleCloud.DEFAULT_NAME_REFRESH_INTERVAL.getSeconds());
        ModuleCloud.startNameRefreshTimer(Duration.ofSeconds(refreshSeconds));
        this.messageEcloudDownloading = this.getCommandResource().getOrSetDefault("messages.ecloud.downloading",
                "&eDownloading &7'&c%this_identifier%&7' &efrom the module cloud&8...");
        this.messageEcloudDownloaded = this.getCommandResource().getOrSetDefault("messages.ecloud.downloaded",
                "&eDownloaded and loaded &7'&c%this_identifier%&7' &ev&c%this_version% &7(&f%this_file%&7)&8!");
        this.messageEcloudDownloadedNotLoaded = this.getCommandResource().getOrSetDefault("messages.ecloud.downloaded-not-loaded",
                "&eDownloaded &7'&c%this_identifier%&7' &ev&c%this_version% &7(&f%this_file%&7)&e, but it could not be loaded&8: &c%this_error%&e. Restart to load it&8.");
        this.messageEcloudFailed = this.getCommandResource().getOrSetDefault("messages.ecloud.failed",
                "&cCould not download &7'&c%this_identifier%&7'&8: &c%this_error%");
        this.messageEcloudUsage = this.getCommandResource().getOrSetDefault("messages.ecloud.usage",
                "&eUsage&8: &f/modules ecloud <download <module> (version)|get <downloads|version|all> <module>>");
        this.messageEcloudGetUsage = this.getCommandResource().getOrSetDefault("messages.ecloud.get.usage",
                "&eUsage&8: &f/modules ecloud get <downloads|version|all> <module>");
        this.messageEcloudGetLooking = this.getCommandResource().getOrSetDefault("messages.ecloud.get.looking",
                "&eLooking up &7'&c%this_identifier%&7' &ein the module cloud&8...");
        this.messageEcloudGetFailed = this.getCommandResource().getOrSetDefault("messages.ecloud.get.failed",
                "&cCould not look up &7'&c%this_identifier%&7'&8: &c%this_error%");
        this.messageEcloudGetDownloads = this.getCommandResource().getOrSetDefault("messages.ecloud.get.downloads",
                "&6%this_name% &7has been downloaded &a%this_downloads% &7time(s) across &f%this_version_count% &7version(s)&8.");
        this.messageEcloudGetVersion = this.getCommandResource().getOrSetDefault("messages.ecloud.get.version",
                "&6%this_name% &7latest version&8: &a%this_version% &8(&7installed here&8: %this_installed%&8)");
        this.messageEcloudGetAll = this.getCommandResource().getOrSetDefault("messages.ecloud.get.all", new ArrayList<>(List.of(
                "&8&m                                                    ",
                "&6&l%this_name% &8» &a%this_version% &8(&7%this_id%&8)",
                "&7&o%this_description%",
                "&8▸ &7Author&8: &f%this_author%   &7License&8: &f%this_license%",
                "&8▸ &7Downloads&8: &a%this_downloads%   &7Size&8: &f%this_size%",
                "&8▸ &7Requires&8: &f%this_requires%   &7Dependencies&8: &f%this_dependencies%",
                "&8▸ &7Published&8: &f%this_created%   &7Updated&8: &f%this_updated%",
                "&8▸ &7Installed here&8: %this_installed%",
                "&8▸ &7SHA-256&8: &7%this_sha256_short%",
                "&8▸ &7Download&8: &b%this_download_url%",
                "&8▸ &7Versions &8(&f%this_version_count%&8):",
                "%this_versions%",
                "&8&m                                                    "
        )));
        this.messageEcloudGetAllVersion = this.getCommandResource().getOrSetDefault("messages.ecloud.get.all-version",
                "   &8- &e%this_version% &8| &a%this_downloads% &7downloads &8| &f%this_size% &8| &7%this_uploaded%");
        this.ecloudGetAllVersions = this.getCommandResource().getOrSetDefault("ecloud.get-all-versions-shown", 5);

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
            case "ecloud":
                runEcloud(context);
                break;
            default:
                context.sendMessage(getWithOther(context.getSender(), messageResultListAll, context.getSender()));
                break;
        }
    }

    /**
     * Handles {@code ecloud download <module> (version)}. The download runs off
     * the command thread; the sender is messaged when it finishes.
     *
     * @param context the command context; argument 0 is {@code ecloud}
     */
    private void runEcloud(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() >= 2 && context.getStringArg(1).equalsIgnoreCase("get")) {
            runEcloudGet(context);
            return;
        }
        if (context.getArgCount() < 3 || ! context.getStringArg(1).equalsIgnoreCase("download")) {
            context.sendMessage(messageEcloudUsage);
            return;
        }

        String name = context.getStringArg(2);
        String version = context.getArgCount() >= 4 ? context.getStringArg(3) : null;
        context.sendMessage(messageEcloudDownloading.replace("%this_identifier%", name));

        ModuleCloud.download(name, version).whenComplete((result, error) -> {
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                String reason = cause instanceof ModuleCloud.CloudException ? cause.getMessage() : String.valueOf(cause);
                context.sendMessage(messageEcloudFailed
                        .replace("%this_identifier%", name)
                        .replace("%this_error%", reason));
                return;
            }
            String message = result.isLoaded() ? messageEcloudDownloaded : messageEcloudDownloadedNotLoaded;
            context.sendMessage(message
                    .replace("%this_identifier%", result.getModuleId())
                    .replace("%this_version%", result.getVersion())
                    .replace("%this_file%", result.getFile().getFileName().toString())
                    .replace("%this_error%", String.valueOf(result.getLoadError())));
        });
    }

    /** What {@code ecloud get} can report. */
    private static final List<String> GET_KINDS = List.of("downloads", "version", "all");

    /**
     * Handles {@code ecloud get <downloads|version|all> <module>}. The lookup runs off the
     * command thread; the sender is messaged when it finishes.
     *
     * @param context the command context; arguments 0 and 1 are {@code ecloud get}
     */
    private void runEcloudGet(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() < 4 || ! GET_KINDS.contains(context.getStringArg(2).toLowerCase(Locale.ROOT))) {
            context.sendMessage(messageEcloudGetUsage);
            return;
        }

        String kind = context.getStringArg(2).toLowerCase(Locale.ROOT);
        String name = context.getStringArg(3);
        context.sendMessage(messageEcloudGetLooking.replace("%this_identifier%", name));

        ModuleCloud.info(name).whenComplete((info, error) -> {
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                String reason = cause instanceof ModuleCloud.CloudException ? cause.getMessage() : String.valueOf(cause);
                context.sendMessage(messageEcloudGetFailed
                        .replace("%this_identifier%", name)
                        .replace("%this_error%", reason));
                return;
            }

            switch (kind) {
                case "downloads":
                    sendLinked(context, fillInfo(messageEcloudGetDownloads, info));
                    break;
                case "version":
                    sendLinked(context, fillInfo(messageEcloudGetVersion, info));
                    break;
                default:
                    for (String line : messageEcloudGetAll) {
                        if (line.trim().equals("%this_versions%")) {
                            List<ModuleCloud.VersionInfo> versions = info.getVersions();
                            int shown = Math.min(versions.size(), Math.max(0, ecloudGetAllVersions));
                            for (int i = 0; i < shown; i++) sendLinked(context, fillVersion(messageEcloudGetAllVersion, versions.get(i)));
                            if (versions.size() > shown) sendLinked(context, "   &8... &7and &f" + (versions.size() - shown) + " &7more");
                            continue;
                        }
                        String filled = fillInfo(line, info);
                        // Optional fields such as the description leave no blank line behind.
                        if (! line.trim().isEmpty() && stripCodes(filled).isEmpty()) continue;
                        sendLinked(context, filled);
                    }
                    break;
            }
        });
    }

    /** Sends {@code line} with any link in it clickable, on every platform. */
    private static void sendLinked(CommandContext<CosmicCommand> context, String line) {
        if (line.isEmpty()) {
            context.sendMessage(" ");
            return;
        }
        ClickableMessage.linkified(line).send(context.getSender());
    }

    private static String stripCodes(String text) {
        return text.replaceAll("(?i)&#[0-9a-f]{6}|[&§][0-9a-fk-or]", "").trim();
    }

    private static String fillInfo(String template, ModuleCloud.ModuleInfo info) {
        String installed = installedVersion(info.getId());
        String installedText;
        if (installed == null) installedText = "&cnot installed";
        else if (installed.equals(info.getVersion())) installedText = "&a" + installed + " &7(latest)";
        else installedText = "&e" + installed + " &7(update available)";

        String sha = info.getSha256();
        return template
                .replace("%this_name%", info.getName())
                .replace("%this_id%", info.getId())
                .replace("%this_description%", info.getDescription())
                .replace("%this_version%", orNone(info.getVersion()))
                .replace("%this_author%", orNone(info.getAuthor()))
                .replace("%this_license%", orNone(info.getLicense()))
                .replace("%this_requires%", orNone(info.getRequires()))
                .replace("%this_dependencies%", info.getDependencies().isEmpty() ? "none" : String.join(", ", info.getDependencies()))
                .replace("%this_downloads%", String.format(Locale.ROOT, "%,d", info.getDownloads()))
                .replace("%this_size%", humanSize(info.getSize()))
                .replace("%this_created%", date(info.getCreatedAt()))
                .replace("%this_updated%", date(info.getUpdatedAt()))
                .replace("%this_installed%", installedText)
                .replace("%this_sha256_short%", sha.length() > 16 ? sha.substring(0, 16) + "…" : orNone(sha))
                .replace("%this_sha256%", orNone(sha))
                .replace("%this_download_url%", orNone(info.getDownloadUrl()))
                .replace("%this_version_count%", String.valueOf(info.getVersions().size()));
    }

    private static String fillVersion(String template, ModuleCloud.VersionInfo version) {
        return template
                .replace("%this_version%", version.getVersion())
                .replace("%this_downloads%", String.format(Locale.ROOT, "%,d", version.getDownloads()))
                .replace("%this_size%", humanSize(version.getSize()))
                .replace("%this_uploaded%", date(version.getUploadedAt()));
    }

    /** The version of the module with Plugin-Id {@code id} loaded on this server, or {@code null}. */
    private static String installedVersion(String id) {
        try {
            org.pf4j.PluginWrapper wrapper = ModuleManager.safePluginManager().getPlugin(id);
            return wrapper == null ? null : wrapper.getDescriptor().getVersion();
        } catch (Throwable e) {
            return null;
        }
    }

    private static String orNone(String value) {
        return value == null || value.isEmpty() ? "none" : value;
    }

    /** The date part of an ISO-8601 timestamp ({@code 2026-10-07T03:26:36Z} → {@code 2026-10-07}). */
    private static String date(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "unknown";
        int t = timestamp.indexOf('T');
        return t > 0 ? timestamp.substring(0, t) : timestamp;
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
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
                    "list",
                    "ecloud"
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
            if (context.getStringArg(0).equalsIgnoreCase("ecloud")) {
                return new ConcurrentSkipListSet<>(List.of("download", "get"));
            }
        }
        if (context.getArgCount() == 3 && context.getStringArg(0).equalsIgnoreCase("ecloud")
                && context.getStringArg(1).equalsIgnoreCase("download")) {
            return new ConcurrentSkipListSet<>(ModuleCloud.getCachedModuleNames());
        }
        if (context.getArgCount() == 3 && context.getStringArg(0).equalsIgnoreCase("ecloud")
                && context.getStringArg(1).equalsIgnoreCase("get")) {
            return new ConcurrentSkipListSet<>(GET_KINDS);
        }
        if (context.getArgCount() == 4 && context.getStringArg(0).equalsIgnoreCase("ecloud")
                && context.getStringArg(1).equalsIgnoreCase("get")) {
            return new ConcurrentSkipListSet<>(ModuleCloud.getCachedModuleNames());
        }
        return new ConcurrentSkipListSet<>();
    }
}
