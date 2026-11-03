package gg.drak.tacoessentials.gui;

import gg.drak.tacoessentials.alias.AliasPrompts;
import gg.drak.tacoessentials.commands.HomeCommands;
import gg.drak.tacoessentials.commands.Msg;
import gg.drak.tacoessentials.data.TacoDatabase;
import gg.drak.tacoessentials.teleport.Teleports;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicItem;
import singularity.gui.GuiClick;
import singularity.gui.GuiIcon;
import singularity.gui.GuiManager;
import singularity.gui.PaginatedGui;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * {@code /homes}: one bed per home slot. On your own menu a click teleports (after the
 * countdown), shift-left-click renames through a chat prompt, shift-right-click deletes, and a
 * click on an empty slot within your limit saves a home there. Another player's menu
 * ({@code /homes <player>}) only teleports, at once, as a staff teleport.
 */
public class HomesMenu extends PaginatedGui {

    /** Most slots shown, so an unlimited player's menu stays a sensible size. */
    private static final int MAX_SHOWN = 500;

    private final String owner;
    /** The owner's name on another player's menu; {@code null} on your own. */
    private final String ownerName;

    public HomesMenu(String owner, String ownerName) {
        super(ownerName == null ? "&6&lYour Homes" : "&6&l" + ownerName + "'s Homes", 6);
        this.owner = owner;
        this.ownerName = ownerName;
    }

    private boolean own() {
        return ownerName == null;
    }

    @Override
    protected List<GuiIcon> buildEntries(CosmicPlayer viewer) {
        TreeMap<Integer, TacoDatabase.Home> homes = new TreeMap<>();
        TacoDatabase.homes(owner).forEach(h -> homes.put(h.getSlot(), h));
        int highest = homes.isEmpty() ? 0 : homes.lastKey();
        int limit = own() ? HomeCommands.limit(viewer) : 0;
        int shown = own() ? (limit == Integer.MAX_VALUE ? highest + 1 : Math.max(limit, highest)) : highest;
        shown = Math.min(shown, MAX_SHOWN);

        List<GuiIcon> entries = new ArrayList<>();
        for (int slot = 1; slot <= shown; slot++) {
            TacoDatabase.Home home = homes.get(slot);
            if (home != null) entries.add(homeIcon(home));
            else if (own()) entries.add(emptyIcon(slot));
        }
        return entries;
    }

    private GuiIcon homeIcon(TacoDatabase.Home home) {
        List<String> lore = new ArrayList<>();
        lore.add("#bdc8c9" + home.getLocation().describe());
        lore.add("");
        lore.add("#bdc8c9Click: #bbff6aTeleport");
        if (own()) {
            lore.add("#bdc8c9Shift-Left: #bbff6aRename");
            lore.add("#bdc8c9Shift-Right: #FF5555Delete");
        }
        CosmicItem item = CosmicItem.of("minecraft:red_bed", "#FFED6AHome " + home.label()).setLore(lore);
        return GuiIcon.of(item, click -> onHome(click, home));
    }

    private GuiIcon emptyIcon(int slot) {
        CosmicItem item = CosmicItem.of("minecraft:light_gray_bed", "#AAAAAAHome " + slot,
                "#bdc8c9Empty", "", "#bdc8c9Click: #bbff6aSet it where you stand");
        return GuiIcon.of(item, click -> {
            CosmicPlayer viewer = click.getViewer();
            try {
                HomeCommands.setHomeHere(viewer, slot, null);
                viewer.sendMessage(Msg.success("Set home #" + slot + "."));
                click.redraw();
            } catch (Msg.Fail fail) {
                viewer.sendMessage(Msg.error(fail.getMessage()));
            }
        });
    }

    private void onHome(GuiClick click, TacoDatabase.Home home) {
        CosmicPlayer viewer = click.getViewer();
        try {
            if (! own()) {
                click.close();
                if (! Teleports.teleport(viewer.getUuid(), home.getLocation(), true)) {
                    throw Msg.fail("That home is in a world that no longer exists (" + home.getLocation().getWorld() + ").");
                }
                viewer.sendMessage(Msg.success("Teleported to " + ownerName + "'s home " + home.label() + "."));
                return;
            }
            switch (click.getType()) {
                case SHIFT_RIGHT:
                    TacoDatabase.deleteHome(owner, home.getSlot());
                    viewer.sendMessage(Msg.success("Deleted home " + home.label() + "."));
                    click.redraw();
                    return;
                case SHIFT_LEFT:
                    click.close();
                    promptRename(viewer, home);
                    return;
                default:
                    click.close();
                    HomeCommands.teleportHome(viewer, home);
            }
        } catch (Msg.Fail fail) {
            viewer.sendMessage(Msg.error(fail.getMessage()));
        }
    }

    private void promptRename(CosmicPlayer viewer, TacoDatabase.Home home) {
        if (! AliasPrompts.open(viewer, text -> rename(viewer, home, text))) return;
        viewer.sendMessage(Msg.info("Type the new name for home " + home.label() + " in chat, &6none &eto clear it, or &6cancel&e."));
    }

    private void rename(CosmicPlayer viewer, TacoDatabase.Home home, String text) {
        try {
            String name = text.equalsIgnoreCase("none") ? null : HomeCommands.validateHomeName(text);
            if (name != null) {
                for (TacoDatabase.Home other : TacoDatabase.homes(owner)) {
                    if (other.getSlot() != home.getSlot() && name.equalsIgnoreCase(other.getName())) {
                        throw Msg.fail("Home " + other.label() + " already has that name.");
                    }
                }
            }
            TacoDatabase.renameHome(owner, home.getSlot(), name);
            viewer.sendMessage(Msg.success(name == null ? "Home #" + home.getSlot() + " no longer has a name."
                    : "Home #" + home.getSlot() + " is now called " + name + "."));
            if (GuiManager.isAvailable()) new HomesMenu(owner, null).open(viewer);
        } catch (Msg.Fail fail) {
            viewer.sendMessage(Msg.error(fail.getMessage()));
        }
    }
}
