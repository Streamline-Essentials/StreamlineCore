package gg.drak.tacoessentials.gui;

import gg.drak.tacoessentials.commands.HomeCommands;
import gg.drak.tacoessentials.commands.Msg;
import gg.drak.tacoessentials.data.TacoDatabase;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicItem;
import singularity.gui.GuiIcon;
import singularity.gui.PaginatedGui;

import java.util.ArrayList;
import java.util.List;

/** {@code /warp} with no name: every warp the viewer may use; a click warps there after the countdown. */
public class WarpsMenu extends PaginatedGui {

    public WarpsMenu() {
        super("&b&lWarps", 6);
    }

    @Override
    protected List<GuiIcon> buildEntries(CosmicPlayer viewer) {
        List<GuiIcon> entries = new ArrayList<>();
        for (TacoDatabase.Named warp : HomeCommands.allowedWarps(viewer)) {
            CosmicItem item = CosmicItem.of("minecraft:ender_pearl", "#38C1FC" + warp.getName(),
                    "#bdc8c9" + warp.getLocation().describe(), "", "#bdc8c9Click: #bbff6aWarp");
            entries.add(GuiIcon.of(item, click -> {
                click.close();
                try {
                    HomeCommands.warpTo(click.getViewer(), warp.getName());
                } catch (Msg.Fail fail) {
                    click.getViewer().sendMessage(Msg.error(fail.getMessage()));
                }
            }));
        }
        return entries;
    }
}
