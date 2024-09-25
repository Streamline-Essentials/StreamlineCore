package singularity.holders;

import lombok.Getter;

import java.util.concurrent.Callable;

@Getter
public class HolderCallable implements Callable<Boolean> {
    private final Callable<Boolean> callable;

    public HolderCallable(Callable<Boolean> callable) {
        this.callable = callable;
    }

    @Override
    public Boolean call() throws Exception {
        return null;
    }
}
