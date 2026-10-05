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

    public CollectionCategory(String id, String name, String icon, String color) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.color = color;
    }
}
