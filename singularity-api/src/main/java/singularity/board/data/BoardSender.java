package singularity.board.data;

import lombok.Getter;

@Getter
public class BoardSender<T> {
    private final T of;

    public BoardSender(T of) {
        this.of = of;
    }

    public String getIdentifier() {
        if (this.of == null)
            return "null";

        if (this.getOf() instanceof String)
            return (String) this.getOf();

        return this.of.getClass().getSimpleName();
    }
}
