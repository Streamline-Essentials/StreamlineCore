package singularity.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Standard chrome items for shell GUIs (border and corner panes, back, paging, help and create
 * buttons), styled like BukkitOfUtils' {@code GuiItems} so Streamline menus and BOU menus match.
 */
public final class GuiItems {
    private GuiItems() {
    }

    /** The black pane that frames shell GUIs. */
    public static CosmicItem borderPane() {
        return cornerPane(CornerColor.BLACK);
    }

    /** A blank-named pane in {@code color}; {@link CornerColor#YELLOW} when {@code null}. */
    public static CosmicItem cornerPane(CornerColor color) {
        return filler((color == null ? CornerColor.YELLOW : color).paneMaterial());
    }

    /** A blank-named item, for filling slots; the black pane when {@code material} is {@code null}. */
    public static CosmicItem filler(String material) {
        return CosmicItem.of(material == null ? CornerColor.BLACK.paneMaterial() : material, " ");
    }

    public static CosmicItem button(String material, String name, List<String> lore) {
        CosmicItem item = CosmicItem.of(material, name);
        if (lore != null && ! lore.isEmpty()) item.setLore(new ArrayList<>(lore));
        return item;
    }

    public static CosmicItem button(String material, String name, String... lore) {
        return button(material, name, lore == null ? Collections.emptyList() : Arrays.asList(lore));
    }

    public static CosmicItem returnButton() {
        return button("minecraft:oak_door", "#FFED6A&lBack",
                "#bdc8c9Return to the previous menu.",
                "",
                "#bdc8c9Click: #bbff6aBack");
    }

    public static CosmicItem closeButton() {
        return button("minecraft:barrier", "#FF5555&lClose",
                "#bdc8c9Close this menu.",
                "",
                "#bdc8c9Click: #bbff6aClose");
    }

    /** @param displayPage the 1-based page the button leads to */
    public static CosmicItem pagePreviousButton(int displayPage) {
        return button("minecraft:oak_button", "#FFED6APrevious Page", "#bdc8c9Page " + displayPage);
    }

    /** @param displayPage the 1-based page the button leads to */
    public static CosmicItem pageNextButton(int displayPage) {
        return button("minecraft:oak_button", "#FFED6ANext Page", "#bdc8c9Page " + displayPage);
    }

    public static CosmicItem helpButton(String... lines) {
        List<String> lore = new ArrayList<>();
        if (lines != null) Collections.addAll(lore, lines);
        return button("minecraft:book", "#FFED6A&lHelp", lore);
    }

    public static CosmicItem createButton(String title, String... lines) {
        List<String> lore = new ArrayList<>();
        if (lines != null) Collections.addAll(lore, lines);
        return button(CornerColor.LIME.paneMaterial(), title, lore);
    }
}
