package net.streamline.api.holders;

import lombok.Getter;

import java.util.concurrent.Callable;

public class HolderCallable implements Callable<Boolean> {
    @Getter
    private final Callable<Boolean> callable;

    public HolderCallable(Callable<Boolean> callable) {
        this.callable = callable;
    }

    @Override
    public Boolean call() throws Exception {
        return null;
    }
}
