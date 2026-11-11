package gg.drak.thebase.objects;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicString extends AtomicReference<String> implements Comparable<AtomicString> {
    public AtomicString(String initialValue) {
        super(initialValue);
    }

    public AtomicString() {
        this("");
    }

    @Override
    public int compareTo(@NotNull AtomicString o) {
        return CharSequence.compare(get(), o.get());
    }
}
