package singularity.objects;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CosmicTitle {
    private final String main;
    private final String sub;

    private long fadeIn;
    private long stay;
    private long fadeOut;

    public CosmicTitle(String main, String sub, long fadeIn, long stay, long fadeOut) {
        this.main = main;
        this.sub = sub;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    public CosmicTitle(String main, String sub) {
        this(main, sub, 100, 100 , 100);
    }
}
