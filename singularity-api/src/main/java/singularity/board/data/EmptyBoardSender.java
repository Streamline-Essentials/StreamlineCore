package singularity.board.data;

/**
 * A {@link BoardSender} that carries no meaningful payload. It uses the sentinel
 * value {@link #EMPTY} to represent the absence of real sender data, and is
 * intended as a placeholder where a non-null {@code BoardSender} is required but
 * no actual sender is available.
 */
public class EmptyBoardSender extends BoardSender<Object> {

    /**
     * Sentinel value used as the payload of an empty board sender.
     * Its string representation serves as the sender identifier.
     */
    public static final Object EMPTY = "EMPTY";

    /**
     * Creates an {@code EmptyBoardSender} backed by the {@link #EMPTY} sentinel value.
     */
    public EmptyBoardSender() {
        super(EMPTY);
    }
}
