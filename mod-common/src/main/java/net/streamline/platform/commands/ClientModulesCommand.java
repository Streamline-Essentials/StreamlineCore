package net.streamline.platform.commands;

import net.minecraft.network.chat.Component;
import net.streamline.platform.text.LegacyText;
import singularity.client.module.ClientModuleCloud;
import singularity.client.module.ClientModuleManager;
import singularity.client.module.WorldModules;
import singularity.registry.ModuleRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@code /slclient modules (list|enable <module>|disable <module>|reload <module>|remove <module>|ecloud (list|info <module>|download <module> (version)|update <module>))}:
 * the game client's own modules, managed while the game runs. Works on any server or none,
 * since client modules never touch one. eCloud only offers modules built for clients: client
 * modules, and world modules (server modules tagged for clients too), which run in the worlds
 * this client hosts and are installed to Streamline's server module folder.
 *
 * <p>Every action runs off the client thread; {@code reply} brings answers back to it.</p>
 */
final class ClientModulesCommand {

    static final String LABEL = "modules";

    private static final List<String> ACTIONS = Arrays.asList("list", "enable", "disable", "reload", "remove", "ecloud");
    private static final List<String> MODULE_ACTIONS = Arrays.asList("enable", "disable", "reload", "remove");
    private static final List<String> CLOUD_ACTIONS = Arrays.asList("list", "info", "download", "update");

    private static final String USAGE = "&eUsage: &f/%s modules (list|enable <module>|disable <module>|reload <module>|remove <module>"
            + "|ecloud (list|info <module>|download <module> (version)|update <module>))";

    private ClientModulesCommand() {}

    /**
     * @param label the label typed, for the usage message
     * @param args  the arguments after {@code modules}
     */
    static void run(String label, String[] args, Consumer<Component> reply) {
        Consumer<String> say = text -> {
            for (String line : text.split("\\R")) reply.accept(LegacyText.parse(line));
        };
        if (! ClientModuleManager.isStarted()) {
            say.accept("&cClient modules are not running in this game.");
            return;
        }

        String action = args.length == 0 ? "list" : args[0].toLowerCase();
        if (action.equals("list")) {
            say.accept(listInstalled());
            return;
        }
        if (action.equals("ecloud")) {
            runCloud(label, Arrays.copyOfRange(args, 1, args.length), say);
            return;
        }
        if (! MODULE_ACTIONS.contains(action) || args.length < 2) {
            say.accept(String.format(USAGE, label));
            return;
        }

        String id = args[1];
        switch (action) {
            case "enable":
                answer(ClientModuleManager.enable(id), say, m -> "&aEnabled &f" + m.getId() + "&a.");
                break;
            case "disable":
                answer(ClientModuleManager.disable(id), say, m -> "&eDisabled &f" + m.getId() + "&e. It stays off until you enable it.");
                break;
            case "reload":
                answer(ClientModuleManager.reload(id), say, m -> "&aReloaded &f" + m.getId() + " &7" + m.getVersion() + "&a.");
                break;
            default:
                if (ClientModuleManager.module(id) == null && ClientModuleManager.unloadedVersion(id) == null
                        && WorldModules.module(id) != null) {
                    answer(WorldModules.remove(id), say, jar -> "&eRemoved world module &f" + jar
                            + "&e. Its configs and data are kept if you install it again.");
                    break;
                }
                answer(ClientModuleManager.remove(id), say, jar -> "&eRemoved &f" + jar
                        + "&e. Its settings and data are kept if you install it again.");
                break;
        }
    }

    private static void runCloud(String label, String[] args, Consumer<String> say) {
        String action = args.length == 0 ? "list" : args[0].toLowerCase();
        switch (action) {
            case "list":
                say.accept("&7Asking eCloud for client modules...");
                answer(ClientModuleCloud.available(), say, ClientModulesCommand::listAvailable);
                return;
            case "info":
                if (args.length < 2) break;
                answer(ClientModuleCloud.info(args[1]), say, ClientModulesCommand::describe);
                return;
            case "download":
                if (args.length < 2) break;
                say.accept("&7Downloading &f" + args[1] + "&7...");
                answer(ClientModuleCloud.download(args[1], args.length > 2 ? args[2] : null), say, ClientModulesCommand::result);
                return;
            case "update":
                if (args.length < 2) break;
                answer(ClientModuleCloud.update(args[1]), say, ClientModulesCommand::result);
                return;
            default:
                break;
        }
        say.accept(String.format(USAGE, label));
    }

    private static <T> void answer(CompletableFuture<T> future, Consumer<String> say, Function<T, String> success) {
        future.whenComplete((value, error) -> {
            if (error == null) {
                say.accept(success.apply(value));
                return;
            }
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            String message = cause.getMessage() == null ? cause.toString() : cause.getMessage();
            say.accept("&c" + message);
        });
    }

    private static String listInstalled() {
        List<ClientModuleManager.Installed> modules = ClientModuleManager.modules();
        Map<String, String> refused = ClientModuleManager.refusals();
        StringBuilder out = new StringBuilder("&eClient modules &7(" + modules.size() + ")&e:");
        for (ClientModuleManager.Installed module : modules) {
            out.append("\n").append(module.isEnabled() ? "&a" : "&8").append(" ● &f").append(module.getId())
                    .append(" &7").append(module.getVersion()).append(module.isEnabled() ? "" : " &8(disabled)");
        }
        for (Map.Entry<String, String> entry : refused.entrySet()) {
            out.append("\n&c ● &f").append(entry.getKey()).append(" &c").append(entry.getValue());
        }
        if (modules.isEmpty() && refused.isEmpty()) {
            out.append("\n&7 None yet. Browse them with &f/slclient modules ecloud list&7, or put jars in &f")
                    .append(ClientModuleManager.folder()).append("&7.");
        }
        List<WorldModules.Installed> world = WorldModules.installed();
        if (! world.isEmpty()) {
            out.append("\n&eWorld modules &7(").append(world.size()).append(", run in the worlds you host)&e:");
            for (WorldModules.Installed module : world) {
                out.append("\n").append(module.isLoaded() ? "&a" : "&7").append(" ● &f").append(module.getId())
                        .append(" &7").append(module.getVersion())
                        .append(module.isLoaded() ? "" : " &8(starts with the next world you open)");
            }
        }
        return out.toString();
    }

    private static String listAvailable(List<ModuleRegistry.ModuleInfo> modules) {
        if (modules.isEmpty()) return "&7eCloud lists no client modules yet.";
        StringBuilder out = new StringBuilder("&eClient modules on eCloud &7(" + modules.size() + ")&e:");
        for (ModuleRegistry.ModuleInfo info : modules) {
            out.append("\n&b ").append(info.getName()).append(" &7").append(info.getVersion())
                    .append(ClientModuleCloud.isWorldModule(info) ? " &3[world]" : "").append(status(info));
            if (! info.getDescription().isEmpty()) out.append("\n&7   ").append(info.getDescription());
        }
        out.append("\n&7Install one with &f/slclient modules ecloud download <module>&7. "
                + "&3[world] &7modules run in the worlds you host.");
        return out.toString();
    }

    private static String describe(ModuleRegistry.ModuleInfo info) {
        StringBuilder out = new StringBuilder("&b" + info.getName() + " &7" + info.getVersion() + status(info));
        if (! info.getDescription().isEmpty()) out.append("\n&7").append(info.getDescription());
        if (ClientModuleCloud.isWorldModule(info)) out.append("\n&3A world module: it runs in the worlds you host.");
        out.append("\n&7Id: &f").append(info.getId());
        if (! info.getAuthor().isEmpty()) out.append(" &7by &f").append(info.getAuthor());
        out.append("\n&7Versions: &f");
        List<String> versions = new ArrayList<>();
        for (ModuleRegistry.VersionInfo version : info.getVersions()) {
            if (version.runsOn(ClientModuleManager.CLIENT_TAG)) versions.add(version.getVersion());
        }
        out.append(versions.isEmpty() ? info.getVersion() : String.join(", ", versions));
        return out.toString();
    }

    /** Whether a registry module is installed here, and whether it is behind. */
    private static String status(ModuleRegistry.ModuleInfo info) {
        String installed = ClientModuleCloud.installedVersion(info.getId());
        if (installed == null) return "";
        if (ClientModuleCloud.updateAvailable(info)) {
            return " &6(installed " + installed + ", update available)";
        }
        return " &a(installed)";
    }

    private static String result(ClientModuleCloud.Result result) {
        switch (result.getStatus()) {
            case INSTALLED:
                if (result.isWorld()) {
                    return "&aInstalled world module &f" + result.getModuleId() + " &7" + result.getVersion()
                            + (result.isEnabled() ? "&a. It is running now." : "&a. It starts with the next world you open.");
                }
                return "&aInstalled &f" + result.getModuleId() + " &7" + result.getVersion()
                        + (result.isEnabled() ? "&a. It is running now." : "&a. It is turned off; enable it with /slclient modules enable " + result.getModuleId() + ".");
            case UPDATED:
                return "&aUpdated &f" + result.getModuleId() + " &7" + result.getOldVersion() + " &a-> &7" + result.getVersion() + "&a.";
            case UP_TO_DATE:
                return "&f" + result.getModuleId() + " &7" + result.getOldVersion() + " &ais up to date.";
            default:
                return "&e" + result.getModuleId() + " is not installed. Install it with /slclient modules ecloud download "
                        + result.getModuleId() + ".";
        }
    }

    /**
     * Suggestions for the word being typed.
     *
     * @param args the arguments after {@code modules}, the last one being typed
     */
    static List<String> suggest(String[] args) {
        if (args.length <= 1) return ACTIONS;
        String action = args[0].toLowerCase();
        if (MODULE_ACTIONS.contains(action)) return args.length == 2 ? installedIds(action.equals("remove")) : Collections.emptyList();
        if (! action.equals("ecloud")) return Collections.emptyList();
        if (args.length == 2) return CLOUD_ACTIONS;
        if (args.length != 3) return Collections.emptyList();
        switch (args[1].toLowerCase()) {
            case "info":
            case "download":
                return ClientModuleCloud.cachedNames();
            case "update":
                return installedIds(true);
            default:
                return Collections.emptyList();
        }
    }

    private static List<String> installedIds(boolean withWorld) {
        List<String> ids = new ArrayList<>();
        for (ClientModuleManager.Installed module : ClientModuleManager.modules()) ids.add(module.getId());
        if (withWorld) {
            for (WorldModules.Installed module : WorldModules.installed()) ids.add(module.getId());
        }
        return ids;
    }
}
