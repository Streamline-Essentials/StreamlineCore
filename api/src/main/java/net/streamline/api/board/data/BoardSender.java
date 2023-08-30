package net.streamline.api.board.data;

public class BoardSender<T> {
    private final T of;

    public BoardSender(T of) {
        this.of = of;
    }

    public T getOf() {
        return of;
    }

    public String getIdentifier() {
        if (this.of == null)
            return "null";

        if (this.getOf() instanceof String)
            return (String) this.getOf();

        return this.of.getClass().getSimpleName();
    }
}
