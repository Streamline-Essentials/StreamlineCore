package host.plas.tags.gui;

import host.plas.tags.StreamlineTags;
import host.plas.tags.data.TagStore;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicItem;
import singularity.gui.GuiIcon;
import singularity.gui.GuiLayout;
import singularity.gui.PaginatedGui;
import singularity.modules.ModuleUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /tags}: every tag the viewer may use, paged; clicking one makes it the active tag.
 */
public class TagsMenu extends PaginatedGui {
    private static final int ROWS = 6;
    private static final int CLEAR_SLOT = GuiLayout.bottomCenter(ROWS);

    public TagsMenu() {
        super(StreamlineTags.getMainConfig().getMenuTitle(), ROWS, GuiLayout.innerSlots(ROWS));
    }

    @Override
    protected void drawFrame(CosmicPlayer viewer) {
        border(CosmicItem.of("minecraft:black_stained_glass_pane", " "));
        CosmicItem corner = CosmicItem.of("minecraft:light_blue_stained_glass_pane", " ");
        for (int slot : GuiLayout.cornerSlots(ROWS)) setItem(slot, corner);

        if (TagStore.usable(viewer).isEmpty()) {
            setItem(22, CosmicItem.of("minecraft:barrier", "#FF5555No tags available",
                    "#AAAAAAYou have no chat tags yet."));
        }

        setIcon(CLEAR_SLOT, CosmicItem.of("minecraft:gray_dye", "#FF5555ᴄʟᴇᴀʀ ᴛᴀɢ",
                "#AAAAAAClick to remove your active tag"), click -> {
            TagStore.clear(click.getViewer().getUuid());
            ModuleUtils.sendMessage(click.getViewer(), StreamlineTags.message("cleared"));
            click.redraw();
        });
    }

    @Override
    protected List<GuiIcon> buildEntries(CosmicPlayer viewer) {
        String activeId = TagStore.activeId(viewer.getUuid()).orElse(null);
        List<GuiIcon> entries = new ArrayList<>();
        for (String tagId : TagStore.usable(viewer)) {
            boolean selected = tagId.equals(activeId);
            CosmicItem item = CosmicItem.of(selected ? "minecraft:lime_dye" : "minecraft:name_tag", TagStore.display(tagId),
                    "#AAAAAAId: &f" + tagId, "", selected ? "#00FC88Currently active" : "#38C1FCClick to activate");
            entries.add(GuiIcon.of(item, click -> {
                CosmicPlayer clicker = click.getViewer();
                if (TagStore.select(clicker, tagId)) {
                    ModuleUtils.sendMessage(clicker, StreamlineTags.message("selected").replace("%tag%", TagStore.display(tagId)));
                } else {
                    ModuleUtils.sendMessage(clicker, StreamlineTags.message("no-access"));
                }
                click.redraw();
            }));
        }
        return entries;
    }
}
