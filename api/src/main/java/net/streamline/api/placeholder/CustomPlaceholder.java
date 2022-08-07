package net.streamline.api.placeholder;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;

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
        SLAPI.getInstance().getPlatform().getRATAPI().registerCustomPlaceholder(this);
    }

    public void unregister() {
        SLAPI.getInstance().getPlatform().getRATAPI().unregisterCustomPlaceholder(this);
    }
}
