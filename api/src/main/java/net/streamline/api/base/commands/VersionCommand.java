package net.streamline.api.base.commands;

import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginWrapper;
import singularity.Singularity;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainConfigHandler;
import singularity.interfaces.ISingularityExtension;
import singularity.modules.ModuleCloud;
import singularity.modules.ModuleLike;
import singularity.modules.ModuleManager;
import singularity.objects.Versioning;
import singularity.registry.ModuleRegistry;
import singularity.updates.CoreUpdater;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * {@code /streamlineversion (core|<module>)} (aliases {@code slversion}, {@code slv}): the
 * version of StreamlineCore, the platform and Minecraft version it runs on and whether a
 * newer core exists, or the version and details of one module and whether eCloud has a
 * newer one. Styled after BukkitOfUtils' {@code /bouversion} and {@code /boup info}.
 */
public class VersionCommand extends CosmicCommand {
    /** The argument that selects the core; also what no argument shows. */
    private static final String CORE = "core";

    private final List<String> messageCore;
    private final String messageCoreChecking;
    private final String messageCoreUpToDate;
    private final String messageCoreAvailable;
    private final String messageCoreStaged;
    private final String messageCoreChecksOff;
    private final String messageCoreNoTarget;
    private final String messageCoreFailed;

    private final List<String> messageModule;
    private final String messageModuleChecking;
    private final String messageModuleCloud;
    private final String messageModuleNotHosted;
    private final String messageModuleCloudFailed;
    private final String messageModuleNotFound;

    public VersionCommand() {
        super(
                "streamline-base",
                "streamlineversion",
                "streamline.command.streamlineversion.default",
                "slversion", "slv", "streamlinever"
        );

        this.messageCore = getCommandResource().getOrSetDefault("messages.core.info", new ArrayList<>(List.of(
                "&7You are running &eversion &a%this_version% &7of &c&lStreamlineCore &7on &b&l%this_platform%&7.",
                "&7Server is &eflagged &7as &a%this_mc_flagged% &e(&fExact&7: &a%this_mc_exact%&e)&7.",
                "   &7-> &bIs &c&lEmpty &bVersioning&7? %this_mc_empty%",
                "   &7-> &bIs &c&lModern &bVersioning&7? %this_mc_modern%",
                "  &f> &ePlatform&7: &b%this_platform% &7(&f%this_server_type%&7)",
                "  &f> &eServer Software&7: &b%this_software%",
                "  &f> &eModules&7: &a%this_modules_enabled% &7enabled &8/ &b%this_modules_loaded% &7loaded",
                "  &f> &eJava&7: &b%this_java%",
                "  &f> &eUptime&7: &b%this_uptime%"
        )));
        this.messageCoreChecking = getCommandResource().getOrSetDefault("messages.core.checking",
                "&7Checking for StreamlineCore updates&8...");
        this.messageCoreUpToDate = getCommandResource().getOrSetDefault("messages.core.up-to-date",
                "&7Update status&8: &aUP_TO_DATE &7(latest: &a%this_latest%&7).");
        this.messageCoreAvailable = getCommandResource().getOrSetDefault("messages.core.available",
                "&7Update status&8: &eBEHIND &7(latest: &a%this_latest%&7). &7Download: &bhttps://modrinth.com/plugin/streamline");
        this.messageCoreStaged = getCommandResource().getOrSetDefault("messages.core.staged",
                "&7Update status&8: &eSTAGED &7(&a%this_latest% &7installs when the server stops).");
        this.messageCoreChecksOff = getCommandResource().getOrSetDefault("messages.core.checks-off",
                "&7Update status&8: &7update checks are turned off.");
        this.messageCoreNoTarget = getCommandResource().getOrSetDefault("messages.core.no-target",
                "&7Update status&8: &7this build does not update itself.");
        this.messageCoreFailed = getCommandResource().getOrSetDefault("messages.core.failed",
                "&cVersion check failed&8: &c%this_error%");

        this.messageModule = getCommandResource().getOrSetDefault("messages.module.info", new ArrayList<>(List.of(
                "&7- %this_colorized_id% &7(%this_state%&7)",
                "  &f> &eVersion&7: &b%this_version%",
                "  &f> &eAuthor&7(&es&7)&7: &b%this_authors%",
                "  &f> &eDescription&7: &f%this_description%",
                "  &f> &ePlatforms&7: &b%this_platforms%",
                "  &f> &eDependencies&7: &b%this_dependencies%",
                "  &f> &eCommands&7: &b%this_commands%",
                "  &f> &eFile&7: &f%this_file%"
        )));
        this.messageModuleChecking = getCommandResource().getOrSetDefault("messages.module.checking",
                "  &f> &eeCloud&7: &eChecking...");
        this.messageModuleCloud = getCommandResource().getOrSetDefault("messages.module.ecloud",
                "  &f> &eeCloud status&7: %this_status% &7(latest: &a%this_latest%&7)");
        this.messageModuleNotHosted = getCommandResource().getOrSetDefault("messages.module.not-hosted",
                "  &f> &eeCloud&7: &7not hosted on eCloud");
        this.messageModuleCloudFailed = getCommandResource().getOrSetDefault("messages.module.ecloud-failed",
                "  &f> &eeCloud&7: &c%this_error%");
        this.messageModuleNotFound = getCommandResource().getOrSetDefault("messages.module.not-found",
                "&cNo module found named &e%this_identifier%&c. &7Usage&8: &f/streamlineversion (core|<module>)");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() < 1 || context.getStringArg(0).equalsIgnoreCase(CORE)) {
            showCore(context);
            return;
        }
        showModule(context, context.getStringArg(0));
    }

    // ---------------------------------------------------------------- core

    private void showCore(CommandContext<CosmicCommand> context) {
        ISingularityExtension platform = Singularity.getInstance().getPlatform();
        Versioning versioning = Versioning.introspect();
        String exact = Versioning.getRawServerVersion();

        Map<String, String> values = new LinkedHashMap<>();
        values.put("%this_version%", orUnknown(platform.getVersion()));
        values.put("%this_platform%", platform.getPlatformDisplayName());
        values.put("%this_server_type%", capitalize(platform.getServerType().name()));
        values.put("%this_software%", orUnknown(safe(platform::getServerSoftware)));
        values.put("%this_mc_flagged%", versioning.isEmpty() ? "unknown" : versioning.toString());
        values.put("%this_mc_exact%", orUnknown(exact));
        values.put("%this_mc_empty%", yesNo(versioning.isEmpty()));
        values.put("%this_mc_modern%", yesNo(versioning.isModern()));
        values.put("%this_modules_enabled%", String.valueOf(ModuleManager.getOnlyMalleableEnabledModuleIdentifiers().size()));
        values.put("%this_modules_loaded%", String.valueOf(ModuleManager.getOnlyMalleableModuleIdentifiers().size()));
        values.put("%this_java%", System.getProperty("java.version", "unknown"));
        values.put("%this_uptime%", formatDuration(ManagementFactory.getRuntimeMXBean().getUptime()));
        values.put("%this_target%", orUnknown(safe(platform::getCoreTarget)));
        for (String line : messageCore) context.sendMessage(fill(line, values));

        sendCoreUpdateStatus(context, platform);
    }

    private void sendCoreUpdateStatus(CommandContext<CosmicCommand> context, ISingularityExtension platform) {
        String staged = CoreUpdater.getStagedVersion();
        if (staged != null) {
            context.sendMessage(messageCoreStaged.replace("%this_latest%", staged));
            return;
        }
        String target = safe(platform::getCoreTarget);
        if (target == null || target.isEmpty()) {
            context.sendMessage(messageCoreNoTarget);
            return;
        }
        MainConfigHandler config = GivenConfigs.getMainConfig();
        if (config.isNoInternet() || ! config.isCoreUpdatesEnabled()) {
            context.sendMessage(messageCoreChecksOff);
            return;
        }

        String current = platform.getVersion();
        context.sendMessage(messageCoreChecking);
        CompletableFuture.supplyAsync(() -> new ModuleRegistry().coreNow(target.toLowerCase(Locale.ROOT)))
                .whenComplete((build, error) -> {
                    if (error != null) {
                        context.sendMessage(messageCoreFailed.replace("%this_error%", reason(error)));
                        return;
                    }
                    String latest = build.getVersion();
                    if (latest.isEmpty() || current == null || ModuleRegistry.compareVersions(latest, current) <= 0) {
                        context.sendMessage(messageCoreUpToDate.replace("%this_latest%", latest.isEmpty() ? orUnknown(current) : latest));
                    } else {
                        context.sendMessage(messageCoreAvailable.replace("%this_latest%", latest));
                    }
                });
    }

    // -------------------------------------------------------------- modules

    private void showModule(CommandContext<CosmicCommand> context, String name) {
        ModuleLike module = findLoaded(name);
        Map<String, String> values = new LinkedHashMap<>();
        String id;
        String version;
        Path jar;

        if (module != null) {
            id = module.getIdentifier();
            PluginWrapper wrapper = wrapper(id);
            PluginDescriptor descriptor = wrapper == null ? null : wrapper.getDescriptor();
            jar = wrapper == null ? null : wrapper.getPluginPath();
            version = descriptor == null ? null : descriptor.getVersion();

            values.put("%this_colorized_id%", (module.isEnabled() ? "&a" : "&c") + id);
            values.put("%this_state%", module.isEnabled() ? "&aEnabled" : "&cDisabled");
            values.put("%this_authors%", orUnknown(module.getAuthorsStringed()));
            values.put("%this_description%", orNone(descriptor == null ? null : descriptor.getPluginDescription()));
            values.put("%this_dependencies%", descriptor == null ? "none" : dependencies(descriptor.getDependencies()));
            values.put("%this_commands%", commands(module));
        } else {
            jar = ModuleManager.findModuleJar(name);
            if (jar == null) {
                context.sendMessage(messageModuleNotFound.replace("%this_identifier%", name));
                return;
            }
            File file = jar.toFile();
            String manifestId = ModuleManager.manifestValue(file, "Plugin-Id");
            id = manifestId == null ? name : manifestId;
            version = ModuleManager.manifestValue(file, "Plugin-Version");

            values.put("%this_colorized_id%", "&e" + id);
            values.put("%this_state%", "&eNot Loaded");
            values.put("%this_authors%", orUnknown(ModuleManager.manifestValue(file, "Plugin-Provider")));
            values.put("%this_description%", orNone(ModuleManager.manifestValue(file, "Plugin-Description")));
            values.put("%this_dependencies%", orNone(ModuleManager.manifestValue(file, "Plugin-Dependencies")));
            values.put("%this_commands%", "none");
        }

        String platforms = jar == null ? null : ModuleManager.manifestValue(jar.toFile(), "Plugin-Platforms");
        values.put("%this_id%", id);
        values.put("%this_version%", orUnknown(version));
        values.put("%this_platforms%", platforms == null || platforms.isEmpty() ? "untagged" : platforms.replace(",", "&7, &b").replace(" ", ""));
        values.put("%this_file%", jar == null ? "built in" : jar.getFileName().toString());
        for (String line : messageModule) context.sendMessage(fill(line, values));

        if (jar == null || version == null) return;
        if (GivenConfigs.getMainConfig().isNoInternet()) return;
        sendModuleCloudStatus(context, id, version, jar);
    }

    private void sendModuleCloudStatus(CommandContext<CosmicCommand> context, String id, String installed, Path jar) {
        context.sendMessage(messageModuleChecking);
        ModuleCloud.info(id).whenComplete((info, error) -> {
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                if (ModuleRegistry.isNotFound(cause)) context.sendMessage(messageModuleNotHosted);
                else context.sendMessage(messageModuleCloudFailed.replace("%this_error%", reason(error)));
                return;
            }
            String status;
            if (ModuleRegistry.updateAvailable(installed, jar, info)) status = "&eBEHIND";
            else if (ModuleRegistry.compareVersions(installed, info.getVersion()) > 0) status = "&bAHEAD";
            else status = "&aUP_TO_DATE";
            context.sendMessage(messageModuleCloud
                    .replace("%this_status%", status)
                    .replace("%this_latest%", orUnknown(info.getVersion())));
        });
    }

    /** The loaded module with this identifier, ignoring case, or {@code null}. */
    private static ModuleLike findLoaded(String name) {
        ModuleLike exact = ModuleManager.getModule(name);
        if (exact != null) return exact;
        for (Map.Entry<String, ModuleLike> entry : ModuleManager.getLoadedModules().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) return entry.getValue();
        }
        return null;
    }

    private static PluginWrapper wrapper(String id) {
        try {
            return ModuleManager.safePluginManager().getPlugin(id);
        } catch (Throwable e) {
            return null;
        }
    }

    private static String dependencies(List<PluginDependency> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) return "none";
        return dependencies.stream()
                .map(d -> d.getPluginId() + (d.isOptional() ? "&7?" : "") + "&b")
                .collect(Collectors.joining("&7, &b"));
    }

    private static String commands(ModuleLike module) {
        ConcurrentSkipListSet<ModuleCommand> commands = ModuleManager.getCommandsForModule(module);
        if (commands.isEmpty()) return "none";
        return commands.stream().map(c -> "/" + c.getBase()).collect(Collectors.joining("&7, &b"));
    }

    // ---------------------------------------------------------------- utils

    private static String fill(String line, Map<String, String> values) {
        String out = line;
        for (Map.Entry<String, String> entry : values.entrySet()) out = out.replace(entry.getKey(), entry.getValue());
        return out;
    }

    private static String reason(Throwable error) {
        Throwable cause = error.getCause() != null ? error.getCause() : error;
        return cause instanceof ModuleRegistry.RegistryException ? cause.getMessage() : String.valueOf(cause);
    }

    private static <T> T safe(java.util.function.Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            return null;
        }
    }

    private static String yesNo(boolean value) {
        return value ? "&aYes" : "&cNo";
    }

    private static String orUnknown(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value;
    }

    private static String orNone(String value) {
        return value == null || value.trim().isEmpty() ? "none" : value;
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    /** {@code 1d 2h 3m 4s}, leaving out leading zero units. */
    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        StringBuilder out = new StringBuilder();
        if (days > 0) out.append(days).append("d ");
        if (days > 0 || hours > 0) out.append(hours).append("h ");
        if (days > 0 || hours > 0 || minutes > 0) out.append(minutes).append("m ");
        out.append(secs).append('s');
        return out.toString();
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        ConcurrentSkipListSet<String> out = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
        if (context.getArgCount() <= 1) {
            out.add(CORE);
            out.addAll(ModuleManager.getOnlyMalleableModuleIdentifiers());
            out.addAll(ModuleManager.getUnloadedExternalModuleIdentifiers());
        }
        return out;
    }
}
