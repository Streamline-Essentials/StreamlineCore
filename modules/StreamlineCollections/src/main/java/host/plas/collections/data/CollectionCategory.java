package host.plas.collections.data;

import lombok.Getter;

/**
 * A group of collections shown together, such as Farming or Mining.
 */
@Getter
public class CollectionCategory {
    private final String id;
    private final String name;
    private final String icon;
    /** The category's accent colour, as {@code #RRGGBB}. */
    private final String color;
    /**
     * Whether this category's amounts are items, so they add up into the category's total and
     * the overall board. Categories counting hours or blocks travelled do not.
     */
    private final boolean summable;

    public CollectionCategory(String id, String name, String icon, String color, boolean summable) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.summable = summable;
    }
}
