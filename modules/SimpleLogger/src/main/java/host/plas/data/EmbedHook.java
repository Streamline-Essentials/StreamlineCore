package host.plas.data;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmbedHook {
    private boolean enabled;
    private int color;
    private String title;
    private String description;
    private String picture;

    public EmbedHook(boolean enabled, int color, String title, String description, String picture) {
        this.enabled = enabled;
        this.color = color;
        this.title = title;
        this.description = description;
        this.picture = picture;
    }
}