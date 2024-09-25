package singularity.board.data;

public class EmptyBoardSender extends BoardSender<Object> {
    public static final Object EMPTY = "EMPTY";

    public EmptyBoardSender() {
        super(EMPTY);
    }
}
