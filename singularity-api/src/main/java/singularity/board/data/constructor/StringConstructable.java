package singularity.board.data.constructor;

public class StringConstructable extends BoardConstructable<String> {
    public StringConstructable(String of, BoardDataConstructor<?, String> constructor) {
        super(of, constructor);
    }

    public StringConstructable(String of) {
        this(of, c -> of);
    }
}
