package host.plas.data.shop;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** A page of the quest shop, listed on its front page. */
@Getter @Setter
public class ShopCategory {
    private final String identifier;
    private String name;
    private String icon = "minecraft:chest";
    private List<String> description = new ArrayList<>();
    private int order = 0;
    /** Sorted by order, then price. */
    private List<ShopEntry> entries = new ArrayList<>();

    public ShopCategory(String identifier) {
        this.identifier = identifier;
        this.name = "&f" + identifier;
    }
}
