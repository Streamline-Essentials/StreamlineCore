package singularity.objects;

import lombok.Getter;
import lombok.Setter;

/**
 * Encapsulates the data needed to display a Minecraft title on a player's
 * screen.
 *
 * <p>A title consists of a large main line, a smaller subtitle, and three
 * timing values (in ticks) that control how quickly the title fades in,
 * how long it stays visible, and how quickly it fades out.
 */
@Getter
@Setter
public class CosmicTitle {

    /**
     * The main (large) title text, displayed in the centre of the screen.
     * Colour codes are supported.  Immutable after construction.
     */
    private final String main;

    /**
     * The subtitle (smaller) text, displayed below the main title.
     * Colour codes are supported.  Immutable after construction.
     */
    private final String sub;

    /**
     * The number of ticks over which the title fades in from transparent.
     * Default is {@code 100} ticks (5 seconds).
     */
    private long fadeIn;

    /**
     * The number of ticks the title remains fully visible.
     * Default is {@code 100} ticks (5 seconds).
     */
    private long stay;

    /**
     * The number of ticks over which the title fades out to transparent.
     * Default is {@code 100} ticks (5 seconds).
     */
    private long fadeOut;

    /**
     * Constructs a title with explicit timing values.
     *
     * @param main    the main title text
     * @param sub     the subtitle text
     * @param fadeIn  ticks for the fade-in animation
     * @param stay    ticks the title stays fully visible
     * @param fadeOut ticks for the fade-out animation
     */
    public CosmicTitle(String main, String sub, long fadeIn, long stay, long fadeOut) {
        this.main = main;
        this.sub = sub;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    /**
     * Constructs a title with default timing values of 100 ticks each for
     * fade-in, stay, and fade-out.
     *
     * @param main the main title text
     * @param sub  the subtitle text
     */
    public CosmicTitle(String main, String sub) {
        this(main, sub, 100, 100 , 100);
    }
}
