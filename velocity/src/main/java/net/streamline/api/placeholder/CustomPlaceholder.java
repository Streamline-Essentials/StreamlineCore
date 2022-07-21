package net.streamline.api.placeholder;

import lombok.Getter;
import lombok.Setter;
import net.streamline.base.Streamline;

public class CustomPlaceholder {
    @Getter @Setter
    private String key;
    @Getter @Setter
    private String value;

    public CustomPlaceholder(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public void register() {
        Streamline.getRATAPI().registerCustomPlaceholder(this);
    }

    public void unregister() {
        Streamline.getRATAPI().unregisterCustomPlaceholder(this);
    }
}
