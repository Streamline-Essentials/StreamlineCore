package net.streamline.api.objects;

import lombok.Getter;
import lombok.Setter;

@Getter
public class StreamlineTitle {
    private final String main;
    private final String sub;

    @Setter
    private long fadeIn;
    @Setter
    private long stay;
    @Setter
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
