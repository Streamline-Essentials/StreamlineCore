package gg.drak.tacoessentials.alias;

import gg.drak.tacoessentials.commands.Msg;
import gg.drak.tacoessentials.commands.Perms;
import gg.drak.tacoessentials.commands.TacoCommand;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.objects.ClickableMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /aliaseditor}: an in-chat editor for custom aliases. Every part of its screens is
 * clickable -- clicks run the editor's own subcommands, and anything that needs text (a new
 * alias name, a command line) is typed into chat through {@link AliasPrompts}.
 *
 * <p>Usage: {@code /aliaseditor (<page>|add (<name>)|new <name> <command>|reload|list|<alias> (<action> ...))},
 * where an alias's actions are {@code commands}, {@code add (<line>)}, {@code editline <i>},
 * {@code setline <i> <line>}, {@code deleteline <i>}, {@code removeline <i>}, {@code moveup <i>},
 * {@code movedown <i>}, {@code toggle <perm|tab>}, {@code customtab (add (<line>)|deleteline <i>|removeline <i>)},
 * {@code confirmdelete} and {@code remove}. Line numbers {@code <i>} count from 0.</p>
 */
public final class AliasEditor {

    static final String NAME = AliasManager.EDITOR_COMMAND;
    private static final String ROOT = "/" + NAME;
    private static final String PERMISSION = Perms.command(NAME);
    private static final int PER_PAGE = 10;
    private static final int SHORTEN = 50;
    /** Words the editor reads as subcommands, so no alias may be named after them. */
    private static final List<String> RESERVED = Arrays.asList("add", "new", "reload", "list");

    private AliasEditor() {}

    public static TacoCommand create() {
        return new TacoCommand(NAME, ctx -> dispatch(ctx.sender(), args(ctx)), AliasEditor::complete);
    }

    private static List<String> args(TacoCommand.Ctx ctx) {
        List<String> args = new ArrayList<>();
        for (int i = 0; i < ctx.count(); i++) args.add(ctx.arg(i));
        return args;
    }

    // ---- subcommands ----

    static void dispatch(CosmicSender sender, List<String> args) throws Msg.Fail {
        if (args.isEmpty()) {
            showList(sender, 1);
            return;
        }
        String first = args.get(0).toLowerCase(Locale.ROOT);
        Integer page = parseInt(first);
        if (page != null && args.size() == 1) {
            showList(sender, page);
            return;
        }
        switch (first) {
            case "add":
                if (args.size() > 2) throw Msg.fail("An alias name is one word, without spaces.");
                if (args.size() == 2) createAlias(sender, args.get(1));
                else promptNewAlias(sender);
                return;
            case "new":
                if (args.size() < 3) throw Msg.fail("Usage: " + ROOT + " new <name> <command>");
                quickNew(sender, args.get(1), join(args, 2));
                return;
            case "reload":
                AliasManager.load();
                sender.sendMessage(Msg.success("Reloaded " + AliasManager.all().size() + " custom aliases from the database."));
                return;
            case "list":
                sendTextList(sender);
                return;
            default:
                break;
        }

        CustomAlias alias = AliasManager.get(first);
        if (alias == null) throw Msg.fail("No alias named " + first + ". Usage: " + ROOT
                + " (<page>|add (<name>)|new <name> <command>|reload|list|<alias> (<action> ...))");
        String action = args.size() >= 2 ? args.get(1).toLowerCase(Locale.ROOT) : "commands";
        switch (action) {
            case "commands":
                showCommands(sender, alias);
                break;
            case "add":
                if (args.size() >= 3) {
                    List<String> commands = new ArrayList<>(alias.getCommands());
                    commands.add(join(args, 2));
                    saveAndShow(sender, alias.withCommands(commands));
                } else {
                    promptAddCommand(sender, alias);
                }
                break;
            case "editline":
                promptEditCommand(sender, alias, index(alias.getCommands(), args, 2));
                break;
            case "setline": {
                int index = index(alias.getCommands(), args, 2);
                if (args.size() < 4) throw Msg.fail("Usage: " + ROOT + " " + alias.getName() + " setline <i> <line>");
                List<String> commands = new ArrayList<>(alias.getCommands());
                commands.set(index, join(args, 3));
                saveAndShow(sender, alias.withCommands(commands));
                break;
            }
            case "deleteline": {
                int index = index(alias.getCommands(), args, 2);
                confirm(sender, "line " + (index + 1), ROOT + " " + alias.getName() + " removeline " + index);
                break;
            }
            case "removeline": {
                List<String> commands = new ArrayList<>(alias.getCommands());
                commands.remove(index(commands, args, 2));
                saveAndShow(sender, alias.withCommands(commands));
                break;
            }
            case "moveup":
            case "movedown": {
                List<String> commands = new ArrayList<>(alias.getCommands());
                int from = index(commands, args, 2);
                int to = from + (action.equals("moveup") ? -1 : 1);
                if (to < 0 || to >= commands.size()) {
                    showCommands(sender, alias);
                    break;
                }
                Collections.swap(commands, from, to);
                saveAndShow(sender, alias.withCommands(commands));
                break;
            }
            case "toggle":
                toggle(sender, alias, args.size() >= 3 ? args.get(2) : "");
                break;
            case "customtab":
                customTab(sender, alias, args);
                break;
            case "confirmdelete":
                confirm(sender, "/" + alias.getName(), ROOT + " " + alias.getName() + " remove");
                break;
            case "remove":
                AliasManager.delete(alias.getName());
                sender.sendMessage(Msg.success("Deleted the alias /" + alias.getName() + "."));
                showList(sender, 1);
                break;
            default:
                throw Msg.fail("Unknown action '" + action + "'. See " + ROOT + " " + alias.getName() + ".");
        }
    }

    private static void createAlias(CosmicSender sender, String rawName) throws Msg.Fail {
        String name = aliasName(rawName);
        CustomAlias existing = AliasManager.get(name);
        if (existing != null) {
            showCommands(sender, existing);
            return;
        }
        if (AliasManager.isTaken(name)) throw Msg.fail("/" + name + " is already a command.");
        CustomAlias alias = CustomAlias.create(name);
        AliasManager.save(alias);
        sender.sendMessage(Msg.success("Created the alias /" + name + "."));
        showCommands(sender, alias);
    }

    private static void quickNew(CosmicSender sender, String rawName, String command) throws Msg.Fail {
        String name = aliasName(rawName);
        if (AliasManager.get(name) != null) throw Msg.fail("The alias /" + name + " already exists.");
        if (AliasManager.isTaken(name)) throw Msg.fail("/" + name + " is already a command.");
        saveAndShow(sender, CustomAlias.create(name).withCommands(Collections.singletonList(command)));
    }

    private static void toggle(CosmicSender sender, CustomAlias alias, String what) throws Msg.Fail {
        switch (what.toLowerCase(Locale.ROOT)) {
            case "perm":
                saveAndShow(sender, alias.withRequiresPerm(! alias.isRequiresPerm()));
                return;
            case "tab":
                saveAndShow(sender, alias.withTabComplete(! alias.isTabComplete()));
                return;
            default:
                throw Msg.fail("Usage: " + ROOT + " " + alias.getName() + " toggle <perm|tab>");
        }
    }

    private static void customTab(CosmicSender sender, CustomAlias alias, List<String> args) throws Msg.Fail {
        String action = args.size() >= 3 ? args.get(2).toLowerCase(Locale.ROOT) : "";
        List<String> tabs = new ArrayList<>(alias.getTabCompletes());
        switch (action) {
            case "":
                showCustomTab(sender, alias);
                return;
            case "add":
                if (args.size() >= 4) {
                    tabs.add(join(args, 3));
                    AliasManager.save(alias.withTabCompletes(tabs));
                    showCustomTab(sender, AliasManager.get(alias.getName()));
                } else {
                    promptAddTab(sender, alias);
                }
                return;
            case "deleteline": {
                int index = index(tabs, args, 3);
                confirm(sender, "tab line " + (index + 1), ROOT + " " + alias.getName() + " customtab removeline " + index);
                return;
            }
            case "removeline":
                tabs.remove(index(tabs, args, 3));
                AliasManager.save(alias.withTabCompletes(tabs));
                showCustomTab(sender, AliasManager.get(alias.getName()));
                return;
            default:
                throw Msg.fail("Usage: " + ROOT + " " + alias.getName() + " customtab (add (<line>)|deleteline <i>|removeline <i>)");
        }
    }

    private static void saveAndShow(CosmicSender sender, CustomAlias alias) {
        AliasManager.save(alias);
        showCommands(sender, alias);
    }

    // ---- prompts ----

    private static void promptNewAlias(CosmicSender sender) throws Msg.Fail {
        CosmicPlayer player = requirePlayer(sender);
        if (! AliasPrompts.open(player, text -> answer(player, Arrays.asList("add", text)))) return;
        sender.sendMessage(Msg.info("Type the new alias's name in chat. Type &6cancel &eto cancel."));
    }

    private static void promptAddCommand(CosmicSender sender, CustomAlias alias) throws Msg.Fail {
        CosmicPlayer player = requirePlayer(sender);
        if (! AliasPrompts.open(player, text -> answer(player, Arrays.asList(alias.getName(), "add", text)))) return;
        new ClickableMessage()
                .text(Msg.info("Type the new line in chat, without the leading /. Type &6cancel &eto cancel. &7(hover for help)"))
                .hover(lineHelp())
                .send(sender);
    }

    private static void promptEditCommand(CosmicSender sender, CustomAlias alias, int index) throws Msg.Fail {
        CosmicPlayer player = requirePlayer(sender);
        String current = alias.getCommands().get(index);
        if (! AliasPrompts.open(player, text -> {
            if (text.equalsIgnoreCase("remove")) answer(player, Arrays.asList(alias.getName(), "removeline", String.valueOf(index)));
            else answer(player, Arrays.asList(alias.getName(), "setline", String.valueOf(index), text));
        })) return;
        new ClickableMessage()
                .text(Msg.info("Type the new text for line " + (index + 1) + " in chat. Type &6cancel &eto cancel or &6remove &eto remove the line. "))
                .text("&6[Paste the old text]").hover(Msg.info("Click to put the old text in your chat box")).suggest(current)
                .send(sender);
    }

    private static void promptAddTab(CosmicSender sender, CustomAlias alias) throws Msg.Fail {
        CosmicPlayer player = requirePlayer(sender);
        if (! AliasPrompts.open(player, text -> answer(player, Arrays.asList(alias.getName(), "customtab", "add", text)))) return;
        new ClickableMessage()
                .text(Msg.info("Type the new tab line in chat. Type &6cancel &eto cancel. &7(hover for help)"))
                .hover(tabHelp())
                .send(sender);
    }

    /** A prompt's answer, run as the matching subcommand once the player is checked to still be allowed to edit. */
    private static void answer(CosmicPlayer player, List<String> args) {
        if (! player.hasPermission(PERMISSION)) {
            player.sendMessage(Msg.error("You no longer have permission to edit aliases."));
            return;
        }
        // Prompted text is split the way a typed command would be, so it reaches dispatch() in the same shape.
        List<String> split = new ArrayList<>();
        for (String arg : args) {
            for (String word : arg.split(" ")) if (! word.isEmpty()) split.add(word);
        }
        try {
            dispatch(player, split);
        } catch (Msg.Fail fail) {
            player.sendMessage(Msg.error(fail.getMessage()));
        }
    }

    // ---- screens ----

    private static void showList(CosmicSender sender, int page) {
        List<CustomAlias> aliases = new ArrayList<>(AliasManager.all());
        int pages = Math.max(1, (aliases.size() + PER_PAGE - 1) / PER_PAGE);
        int current = Math.min(Math.max(1, page), pages);
        int start = (current - 1) * PER_PAGE;

        sender.sendMessage(splitter("Aliases"));
        for (int i = start; i < Math.min(start + PER_PAGE, aliases.size()); i++) {
            CustomAlias alias = aliases.get(i);
            new ClickableMessage()
                    .text(" &c[X]").hover("&cDelete /" + alias.getName()).run(ROOT + " " + alias.getName() + " confirmdelete")
                    .text(" " + number(i + 1))
                    .text("&6" + alias.getName()).hover(commandsHover(alias)).run(ROOT + " " + alias.getName())
                    .send(sender);
        }
        new ClickableMessage()
                .text(" &a[+]").hover("&aAdd a new alias").run(ROOT + " add")
                .text(" " + number(aliases.size() + 1))
                .send(sender);

        if (pages > 1) {
            int prev = current > 1 ? current - 1 : pages;
            int next = current < pages ? current + 1 : 1;
            new ClickableMessage()
                    .text("&8----<< &7Prev ").hover("&eClick").run(ROOT + " " + prev)
                    .text("&8" + current + "&7/&8" + pages).hover("&6" + aliases.size() + " aliases")
                    .text("&7 Next &8>>----").hover("&eClick").run(ROOT + " " + next)
                    .send(sender);
        }
    }

    private static void showCommands(CosmicSender sender, CustomAlias alias) {
        String base = ROOT + " " + alias.getName();
        sender.sendMessage(splitter("/" + alias.getName()));

        new ClickableMessage()
                .text(toggleColor(alias.isRequiresPerm()) + " Permission")
                .hover(alias.isRequiresPerm()
                        ? "&7Click to change\n&7Needs &6" + alias.getPermission() + " &7to use"
                        : "&7Click to change\n&7Anyone with &6" + Perms.ALIAS + " &7can use it")
                .run(base + " toggle perm")
                .text(toggleColor(alias.isTabComplete()) + " TabComplete")
                .hover(alias.isTabComplete() ? "&7Click to change\n&7When disabled, tab completion will not work" : "&7Click to change")
                .run(base + " toggle tab")
                .text(toggleColor(! alias.getTabCompletes().isEmpty()) + " CustomTab")
                .hover(alias.getTabCompletes().isEmpty() ? "&7No custom tab lines; click to add some" : String.join("\n", alias.getTabCompletes()))
                .run(base + " customtab")
                .send(sender);

        new ClickableMessage()
                .text(splitter("Commands")).hover("&7Back to all aliases").run(ROOT)
                .send(sender);

        List<String> commands = alias.getCommands();
        for (int i = 0; i < commands.size(); i++) {
            new ClickableMessage()
                    .text(" &c[X] ").hover("&cDelete line " + (i + 1)).run(base + " deleteline " + i)
                    .text(number(i + 1))
                    .text("&7⇑").hover("&7Up").run(base + " moveup " + i)
                    .text("&7⇓ ").hover("&7Down").run(base + " movedown " + i)
                    .text("&f" + shorten(commands.get(i))).hover("&eClick to edit\n&7" + commands.get(i)).run(base + " editline " + i)
                    .send(sender);
        }
        new ClickableMessage()
                .text(" &a[+] ").hover("&aAdd a line").run(base + " add")
                .text(number(commands.size() + 1))
                .send(sender);
    }

    private static void showCustomTab(CosmicSender sender, CustomAlias alias) {
        String base = ROOT + " " + alias.getName();
        sender.sendMessage(splitter("/" + alias.getName()));
        new ClickableMessage()
                .text("&2TabCompletes: ").hover(tabHelp())
                .send(sender);

        List<String> tabs = alias.getTabCompletes();
        for (int i = 0; i < tabs.size(); i++) {
            new ClickableMessage()
                    .text(" &c[X] ").hover("&cDelete tab line " + (i + 1)).run(base + " customtab deleteline " + i)
                    .text(number(i + 1))
                    .text("&f" + tabs.get(i)).hover("&eClick to put it in your chat box").suggest(tabs.get(i))
                    .send(sender);
        }
        new ClickableMessage()
                .text(" &a[+] ").hover("&aAdd a tab line").run(base + " customtab add")
                .text(number(tabs.size() + 1))
                .send(sender);
        new ClickableMessage()
                .text(splitter("Commands")).hover("&7Back to the commands").run(base)
                .send(sender);
    }

    private static void confirm(CosmicSender sender, String what, String command) {
        new ClickableMessage()
                .text("&eClick to confirm removal of &6" + what).hover("&eClick").run(command)
                .send(sender);
    }

    private static void sendTextList(CosmicSender sender) {
        if (AliasManager.all().isEmpty()) {
            sender.sendMessage(Msg.muted("No custom aliases configured."));
            return;
        }
        sender.sendMessage(Msg.info("Custom aliases:"));
        for (CustomAlias alias : AliasManager.all()) {
            sender.sendMessage("&a- /" + alias.getName() + " &7(" + alias.getCommands().size() + " lines)");
        }
    }

    // ---- formatting ----

    private static String splitter(String title) {
        return "&7------------------ &6" + title + " &7------------------";
    }

    private static String number(int n) {
        return (n < 10 ? "&80" : "&8") + n + ". ";
    }

    private static String toggleColor(boolean on) {
        return on ? "&a" : "&c";
    }

    private static String commandsHover(CustomAlias alias) {
        if (alias.getCommands().isEmpty()) return "&7No commands configured";
        StringBuilder out = new StringBuilder();
        for (String command : alias.getCommands()) {
            if (out.length() > 0) out.append('\n');
            out.append("&7/&f").append(command);
        }
        return out.toString();
    }

    private static String shorten(String value) {
        String flat = value.replace('\n', ' ').trim();
        return flat.length() <= SHORTEN ? flat : flat.substring(0, SHORTEN) + "...";
    }

    private static String lineHelp() {
        return "&eA line is a command, run as whoever ran the alias, or:\n"
                + "&edelay! 5 &6runs the lines after it 5 seconds later\n"
                + "&easConsole! <command> &6runs it from the console\n"
                + "&emsg! <text> &6/ &ebroadcast! <text> &6sends text\n"
                + "&eperm:<node>! <line> &6runs the line only with that permission\n"
                + "&e$1 &6is the alias's first argument, &e$1- &6the first one onward\n"
                + "&e[playerName] [currentWorld] [currentX] &6and placeholders are filled in";
    }

    private static String tabHelp() {
        return "&eCandidates per argument, arguments split by &6- &eand choices by &6,\n"
                + "&ea,b-[playername] &6offers a or b, then a player name\n"
                + "&eSpecial: &6[playername] [worlds] [customalias]";
    }

    // ---- helpers ----

    private static String aliasName(String raw) throws Msg.Fail {
        String name = TacoCommand.normalizeName(raw, "alias");
        if (RESERVED.contains(name) || parseInt(name) != null) throw Msg.fail("'" + name + "' cannot be an alias name: the editor uses it.");
        return name;
    }

    private static CosmicPlayer requirePlayer(CosmicSender sender) throws Msg.Fail {
        if (sender instanceof CosmicPlayer) return (CosmicPlayer) sender;
        throw Msg.fail("Only players can answer editor prompts; pass the text as an argument instead.");
    }

    private static int index(List<String> lines, List<String> args, int position) throws Msg.Fail {
        Integer index = args.size() > position ? parseInt(args.get(position)) : null;
        if (index == null || index < 0 || index >= lines.size()) throw Msg.fail("There is no line " + (args.size() > position ? args.get(position) : "") + ".");
        return index;
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String join(List<String> args, int from) {
        return String.join(" ", args.subList(Math.min(from, args.size()), args.size())).trim();
    }

    private static List<String> complete(TacoCommand.Ctx ctx, int arg) {
        List<String> out = new ArrayList<>();
        if (arg == 0) {
            out.addAll(Arrays.asList("add", "new", "reload", "list"));
            AliasManager.all().forEach(a -> out.add(a.getName()));
        } else if (arg == 1 && AliasManager.get(ctx.arg(0)) != null) {
            out.addAll(Arrays.asList("commands", "add", "customtab", "toggle", "confirmdelete"));
        } else if (arg == 2 && "toggle".equalsIgnoreCase(ctx.arg(1))) {
            out.addAll(Arrays.asList("perm", "tab"));
        } else if (arg == 2 && "customtab".equalsIgnoreCase(ctx.arg(1))) {
            out.add("add");
        }
        return out;
    }
}
