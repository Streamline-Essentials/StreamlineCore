package net.streamline.api.board;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.board.data.BoardData;

import java.util.concurrent.ConcurrentSkipListSet;

public class MessageBoard<T> {
    @Getter
    private final T of;

    @Getter @Setter
    private ConcurrentSkipListSet<BoardData> data;

    public MessageBoard(T of, ConcurrentSkipListSet<BoardData> data) {
        this.of = of;
        this.data = data;
    }

    public MessageBoard(T of) {
        this(of, new ConcurrentSkipListSet<>());
    }

    public MessageBoard<T> withData(ConcurrentSkipListSet<BoardData> data) {
        this.data = data;
        return this;
    }

    public void unpost(String identifier) {
        this.data.removeIf(data -> data.getSender().getIdentifier().equals(identifier));
    }

    public void unpost(Class<?> clazz) {
        this.data.removeIf(data -> data.getSender().getOf().getClass().equals(clazz));
    }

    public void unpost(BoardData data) {
        unpost(data.getIdentifier());
    }

    public void post(BoardData data) {
        unpost(data);

        this.data.add(data);
    }
}
