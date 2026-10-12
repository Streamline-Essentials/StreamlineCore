package host.plas.tags.commands;

import host.plas.tags.StreamlineTags;
import host.plas.tags.data.TagStore;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.ISingularityExtension;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * The tag admin command. It is {@code /tag} on Spigot and proxies; on the mod loaders a Streamline
 * label replaces the vanilla command of the same name for commands and datapack functions alike,
 * so there it is only {@code /tagadmin}, leaving vanilla's {@code /tag} alone.
 *
 * <p>{@code /tag (create <id> <display>|set <id> <display>|remove <id>|grant <player> <id>|revoke <player> <id>|list
 * |group create <group>|group delete <group>|group add <group> <id>|group remove <group> <id>
 * |group grant <player> <group>|reload)}</p>
 */
public class TagCommand extends ModuleCommand {
    private static final String ID_PATTERN = "[a-z0-9_-]+";

    public TagCommand() {
        super(StreamlineTags.getInstance(), modLoader() ? "tagadmin" : "tag", "streamline.command.tagadmin.default",
                modLoader() ? new String[0] : new String[] { "tagadmin" });
    }

    private static boolean modLoader() {
        ISingularityExtension.PlatformType platform = ModuleUtils.getPlatformType();
        return platform == ISingularityExtension.PlatformType.FABRIC
                || platform == ISingularityExtension.PlatformType.FORGE
                || platform == ISingularityExtension.PlatformType.NEOFORGE;
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        String[] args = args(context);
        String sub = arg(args, 0).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create":
            case "set":
                define(context, args, sub.equals("create"));
                break;
            case "remove": {
                String id = arg(args, 1).toLowerCase(Locale.ROOT);
                if (! TagStore.exists(id)) {
                    send(context, "unknown");
                    return;
                }
                TagStore.remove(id);
                send(context, "removed", "%id%", id);
                break;
            }
            case "grant":
            case "revoke":
                grant(context, args, sub.equals("grant"));
                break;
            case "list":
                for (String line : StreamlineTags.getMainConfig().getMessageLines("list")) {
                    context.sendMessage(line.replace("%tags%", String.join(", ", TagStore.ids()))
                            .replace("%groups%", String.join(", ", TagStore.groupIds())));
                }
                break;
            case "group":
                group(context, args);
                break;
            case "reload":
                TagStore.refresh(new ArrayList<>(UserUtils.getOnlinePlayers().keySet())).thenRun(() -> {
                    StreamlineTags.getInstance().restartRefresh();
                    send(context, "reloaded", "%count%", String.valueOf(TagStore.ids().size()));
                });
                break;
            default:
                usage(context);
                break;
        }
    }

    private void define(CommandContext<CosmicCommand> context, String[] args, boolean create) {
        if (args.length < 3) {
            usage(context);
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (! id.matches(ID_PATTERN)) {
            send(context, "invalid-id");
            return;
        }
        if (create == TagStore.exists(id)) {
            send(context, create ? "exists" : "unknown");
            return;
        }
        String display = unquote(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
        TagStore.create(id, display);
        send(context, create ? "created" : "updated", "%id%", id, "%tag%", display);
    }

    private void grant(CommandContext<CosmicCommand> context, String[] args, boolean grant) {
        if (args.length < 3) {
            usage(context);
            return;
        }
        Optional<String> target = uuidOf(context, args[1]);
        if (target.isEmpty()) return;
        String uuid = target.get();
        String name = args[1];
        String id = args[2].toLowerCase(Locale.ROOT);
        if (! TagStore.exists(id)) {
            send(context, "unknown");
            return;
        }
        if (! grant) {
            TagStore.revoke(uuid, id);
            send(context, "revoked", "%id%", id, "%player%", name);
            return;
        }
        TagStore.grant(uuid, id).thenAccept(granted -> {
            if (! granted) {
                send(context, "already-granted");
                return;
            }
            send(context, "granted", "%id%", id, "%player%", name);
            notifyReceived(uuid);
        });
    }

    private void group(CommandContext<CosmicCommand> context, String[] args) {
        String action = arg(args, 1).toLowerCase(Locale.ROOT);
        switch (action) {
            case "create": {
                String group = arg(args, 2).toLowerCase(Locale.ROOT);
                if (! group.matches(ID_PATTERN)) {
                    send(context, "invalid-id");
                    return;
                }
                if (TagStore.groupExists(group)) {
                    send(context, "group-exists");
                    return;
                }
                TagStore.createGroup(group);
                send(context, "group-created", "%group%", group);
                break;
            }
            case "delete": {
                String group = arg(args, 2).toLowerCase(Locale.ROOT);
                if (! TagStore.groupExists(group)) {
                    send(context, "group-unknown");
                    return;
                }
                TagStore.removeGroup(group);
                send(context, "group-deleted", "%group%", group);
                break;
            }
            case "add":
            case "remove": {
                String group = arg(args, 2).toLowerCase(Locale.ROOT);
                String id = arg(args, 3).toLowerCase(Locale.ROOT);
                if (! TagStore.groupExists(group)) {
                    send(context, "group-unknown");
                    return;
                }
                if (! TagStore.exists(id)) {
                    send(context, "unknown");
                    return;
                }
                boolean add = action.equals("add");
                boolean changed = add ? TagStore.addToGroup(group, id) : TagStore.removeFromGroup(group, id);
                send(context, changed ? (add ? "group-added" : "group-removed") : "no-change", "%group%", group, "%id%", id);
                break;
            }
            case "grant": {
                if (args.length < 4) {
                    usage(context);
                    return;
                }
                Optional<String> target = uuidOf(context, args[2]);
                if (target.isEmpty()) return;
                String uuid = target.get();
                String name = args[2];
                String group = args[3].toLowerCase(Locale.ROOT);
                if (! TagStore.groupExists(group)) {
                    send(context, "group-unknown");
                    return;
                }
                List<CompletableFuture<Boolean>> grants = new ArrayList<>();
                for (String id : TagStore.tagsInGroup(group)) grants.add(TagStore.grant(uuid, id));
                CompletableFuture.allOf(grants.toArray(new CompletableFuture[0])).thenRun(() -> {
                    long granted = grants.stream().filter(CompletableFuture::join).count();
                    send(context, "group-granted", "%count%", String.valueOf(granted), "%group%", group, "%player%", name);
                    if (granted > 0) notifyReceived(uuid);
                });
                break;
            }
            default:
                usage(context);
                break;
        }
    }

    private static void notifyReceived(String uuid) {
        UserUtils.getPlayer(uuid).filter(CosmicPlayer::isOnline)
                .ifPresent(player -> ModuleUtils.sendMessage(player, StreamlineTags.message("received")));
    }

    /** The UUID of a player who has joined, telling the sender when there is none. */
    private static Optional<String> uuidOf(CommandContext<CosmicCommand> context, String name) {
        Optional<String> uuid = UserUtils.getUUIDFromName(name);
        if (uuid.isEmpty()) send(context, "unknown-player", "%player%", name);
        return uuid;
    }

    private static void usage(CommandContext<CosmicCommand> context) {
        for (String line : StreamlineTags.getMainConfig().getMessageLines("usage")) context.sendMessage(line);
    }

    private static void send(CommandContext<CosmicCommand> context, String key, String... replacements) {
        String message = StreamlineTags.message(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) message = message.replace(replacements[i], replacements[i + 1]);
        context.sendMessage(message);
    }

    private static String unquote(String text) {
        String trimmed = text.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String arg(String[] args, int index) {
        return index < args.length ? args[index] : "";
    }

    /**
     * The arguments typed, without blank ones: a trailing or doubled space arrives as an empty
     * argument on some platforms.
     */
    private static String[] args(CommandContext<CosmicCommand> context) {
        List<String> args = new ArrayList<>();
        for (String arg : context.getArgsArray()) {
            if (arg != null && ! arg.trim().isEmpty()) args.add(arg.trim());
        }
        return args.toArray(new String[0]);
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        String[] args = context.getArgsArray();
        int count = Math.max(1, args.length);
        String sub = arg(args, 0).toLowerCase(Locale.ROOT);
        String groupAction = arg(args, 1).toLowerCase(Locale.ROOT);
        Collection<String> options;
        switch (count) {
            case 1:
                options = List.of("create", "set", "remove", "grant", "revoke", "list", "group", "reload");
                break;
            case 2:
                if (sub.equals("set") || sub.equals("remove")) options = TagStore.ids();
                else if (sub.equals("grant") || sub.equals("revoke")) options = ModuleUtils.getOnlinePlayerNames();
                else if (sub.equals("group")) options = List.of("create", "delete", "add", "remove", "grant");
                else options = List.of();
                break;
            case 3:
                if (sub.equals("grant") || sub.equals("revoke")) options = TagStore.ids();
                else if (sub.equals("group")) options = groupAction.equals("grant") ? ModuleUtils.getOnlinePlayerNames() : TagStore.groupIds();
                else options = List.of();
                break;
            case 4:
                if (sub.equals("group")) options = groupAction.equals("grant") ? TagStore.groupIds() : TagStore.ids();
                else options = List.of();
                break;
            default:
                options = List.of();
                break;
        }
        String partial = args.length == 0 ? "" : args[args.length - 1];
        return ModuleUtils.getCompletion(new ConcurrentSkipListSet<>(options), partial);
    }
}
