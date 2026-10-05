package gg.drak.tacoessentials.commands;

import singularity.interfaces.IGameplayHandler.Workstation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/**
 * {@code /workstation <type>} and one shortcut per type ({@code /craft}, {@code /anvil}, …).
 * A type's permission is its shortcut's command node, so {@code tacoessentials.command.anvil}
 * allows both {@code /anvil} and {@code /workstation anvil}.
 */
public final class WorkstationCommands {

    /** Shortcut command name per workstation. {@code /enchant} is avoided: vanilla owns it. */
    private static final Map<Workstation, String> SHORTCUTS = new LinkedHashMap<>();

    /** Every name {@code /workstation} accepts for a type, the shortcut name first. */
    private static final Map<String, Workstation> NAMES = new LinkedHashMap<>();

    static {
        shortcut(Workstation.CRAFTING, "craft", "crafting", "workbench", "craftingtable");
        shortcut(Workstation.ANVIL, "anvil");
        shortcut(Workstation.SMITHING, "smithing", "smithingtable");
        shortcut(Workstation.GRINDSTONE, "grindstone");
        shortcut(Workstation.STONECUTTER, "stonecutter");
        shortcut(Workstation.CARTOGRAPHY, "cartography", "cartographytable");
        shortcut(Workstation.LOOM, "loom");
        shortcut(Workstation.ENCHANTING, "enchanting", "enchantingtable");
    }

    private static void shortcut(Workstation type, String command, String... aliases) {
        SHORTCUTS.put(type, command);
        NAMES.put(command, type);
        for (String alias : aliases) NAMES.put(alias, type);
    }

    private WorkstationCommands() {}

    public static List<TacoCommand> create() {
        List<TacoCommand> commands = new ArrayList<>();
        commands.add(new TacoCommand("workstation", WorkstationCommands::workstation, WorkstationCommands::allowedNames));
        SHORTCUTS.forEach((type, command) -> commands.add(new TacoCommand(command, ctx -> open(ctx, type))));
        return commands;
    }

    private static void workstation(TacoCommand.Ctx ctx) throws Msg.Fail {
        String usage = "/workstation <" + String.join("|", SHORTCUTS.values()) + ">";
        String name = ctx.require(0, usage).toLowerCase(Locale.ROOT);
        Workstation type = NAMES.get(name);
        if (type == null) throw Msg.fail("Unknown workstation '" + name + "'. Usage: " + usage);
        if (! ctx.has(Perms.command(SHORTCUTS.get(type)))) throw Msg.fail("You may not open the " + SHORTCUTS.get(type) + " workstation.");
        open(ctx, type);
    }

    private static void open(TacoCommand.Ctx ctx, Workstation type) throws Msg.Fail {
        if (! gameplay().openWorkstation(ctx.player().getUuid(), type)) {
            throw Msg.fail("This server cannot open that workstation remotely.");
        }
    }

    /** The shortcut names of the types the sender may open. */
    private static List<String> allowedNames(TacoCommand.Ctx ctx, int arg) {
        if (arg != 0) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        SHORTCUTS.values().forEach(command -> {
            if (ctx.has(Perms.command(command))) names.add(command);
        });
        return names;
    }
}
