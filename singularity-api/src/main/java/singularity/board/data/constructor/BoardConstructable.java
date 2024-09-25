package singularity.board.data.constructor;

import lombok.Getter;
import lombok.Setter;
import singularity.utils.MessageUtils;

@Setter
@Getter
public class BoardConstructable<C> {
    private C of;
    private BoardDataConstructor<?, C> constructor;

    public BoardConstructable(C of, BoardDataConstructor<?, C> constructor) {
        this.of = of;
        this.constructor = constructor;
    }

    public BoardConstructable<C> withConstructor(BoardDataConstructor<?, C> constructor) {
        this.constructor = constructor;
        return this;
    }

    public C construct() {
        try {
            return constructor.apply(null);
        } catch (Exception e) {
            MessageUtils.logDebug(e);
            return null;
        }
    }

    public <D> C construct(D data) {
        try {
            return ((BoardDataConstructor<D, C>) constructor).apply(data);
        } catch (Exception e) {
            MessageUtils.logDebug(e);
            return null;
        }
    }
}
