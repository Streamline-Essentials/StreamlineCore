package host.plas.data;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PlainHook {
    private boolean enabled;
    private String name;
    private String picture;
    private String content;

    public PlainHook(boolean enabled, String name, String picture, String content) {
        this.enabled = enabled;
        this.name = name;
        this.picture = picture;
        this.content = content;
    }
}