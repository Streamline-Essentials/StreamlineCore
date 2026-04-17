package singularity.board;

import lombok.Getter;
import lombok.Setter;
import singularity.board.data.BoardData;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A thread-safe board that associates an owner of type {@code T} with a sorted set of
 * {@link BoardData} entries. Entries can be posted (inserted or replaced) and unposted
 * (removed) by identifier, class, or {@code BoardData} instance.
 *
 * @param <T> the type of the object that owns or is represented by this board
 */
@Getter
public class MessageBoard<T> {

    /** The owner or subject of this message board. */
    private final T of;

    /** The thread-safe, sorted set of data entries currently posted to this board. */
    @Setter
    private ConcurrentSkipListSet<BoardData> data;

    /**
     * Creates a {@code MessageBoard} with an explicit initial data set.
     *
     * @param of   the owner of this board
     * @param data the initial set of board data entries
     */
    public MessageBoard(T of, ConcurrentSkipListSet<BoardData> data) {
        this.of = of;
        this.data = data;
    }

    /**
     * Creates a {@code MessageBoard} with an empty data set.
     *
     * @param of the owner of this board
     */
    public MessageBoard(T of) {
        this(of, new ConcurrentSkipListSet<>());
    }

    /**
     * Replaces the board's data set and returns {@code this} for chaining.
     *
     * @param data the new set of board data entries
     * @return this instance
     */
    public MessageBoard<T> withData(ConcurrentSkipListSet<BoardData> data) {
        this.data = data;
        return this;
    }

    /**
     * Removes all entries whose sender identifier matches the given string.
     *
     * @param identifier the sender identifier to match for removal
     */
    public void unpost(String identifier) {
        this.data.removeIf(data -> data.getSender().getIdentifier().equals(identifier));
    }

    /**
     * Removes all entries whose sender's underlying object is an instance of the
     * given class.
     *
     * @param clazz the class to match against each sender's payload type
     */
    public void unpost(Class<?> clazz) {
        this.data.removeIf(data -> data.getSender().getOf().getClass().equals(clazz));
    }

    /**
     * Removes the entry matching the given {@link BoardData}'s identifier.
     *
     * @param data the board data entry whose identifier is used for removal
     */
    public void unpost(BoardData data) {
        unpost(data.getIdentifier());
    }

    /**
     * Posts a {@link BoardData} entry to the board, replacing any existing entry
     * with the same identifier before adding the new one.
     *
     * @param data the board data entry to post
     */
    public void post(BoardData data) {
        unpost(data);

        this.data.add(data);
    }
}
