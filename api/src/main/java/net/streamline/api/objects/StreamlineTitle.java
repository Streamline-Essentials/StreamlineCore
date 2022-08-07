package net.streamline.api.objects;

import lombok.Getter;
import lombok.Setter;

public class StreamlineTitle {
    @Getter
    private final String main;
    @Getter
    private final String sub;

    @Getter @Setter
    private long fadeIn;
    @Getter @Setter
    private long stay;
    @Getter @Setter
    private long fadeOut;

    public StreamlineTitle(String main, String sub, long fadeIn, long stay, long fadeOut) {
        this.main = main;
        this.sub = sub;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    public StreamlineTitle(String main, String sub) {
        this(main, sub, 100, 100 , 100);
    }
}
