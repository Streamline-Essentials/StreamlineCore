package net.streamline.api.placeholder;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.base.module.BaseModule;
import net.streamline.api.objects.AtomicString;
import net.streamline.api.utils.MatcherUtils;
import net.streamline.api.utils.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class RATResult implements Comparable<RATResult> {
    @Getter @Setter
    private RATReplacement replacement;
    @Getter @Setter
    private int timedReplaced;

    public RATResult(RATReplacement replacement) {
        setReplacement(replacement);
    }

    public int parse(AtomicString on) {
        String temp;
        do {
            temp = on.get();
            on.set(on.get().replaceFirst(MatcherUtils.makeLiteral(getReplacement().getTotal()), getReplacement().getReplacement()));
            if (! temp.equals(on.get())) increment();
        } while (! temp.equals(on.get()));

        return getTimedReplaced();
    }

    public void increment() {
        setTimedReplaced(getTimedReplaced() + 1);
    }

    @Override
    public int compareTo(@NotNull RATResult o) {
        return CharSequence.compare(getReplacement().getTotal(), o.getReplacement().getTotal());
    }

    @Override
    public String toString() {
        return "RATResult[ " + getReplacement().toString() + " ]";
    }
}
