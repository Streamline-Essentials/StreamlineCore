package host.plas.database;

import host.plas.StreamlineMessaging;
import host.plas.savables.SavableChatter;
import singularity.data.console.CosmicSender;
import singularity.database.modules.DBKeeper;
import singularity.loading.Loader;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MyLoader extends Loader<SavableChatter> {
    private static MyLoader instance;

    public static MyLoader getInstance() {
        if (instance == null) instance = new MyLoader();

        return instance;
    }

    @Override
    public DBKeeper<SavableChatter> getKeeper() {
        return StreamlineMessaging.getKeeper();
    }

    @Override
    public SavableChatter getConsole() {
        CosmicSender console = UserUtils.getConsole();
        Optional<SavableChatter> optional = getLoaded().stream().filter(a -> a.getIdentifier().equals(console.getUuid())).findFirst();
        if (optional.isPresent()) return optional.get();

        CompletableFuture<SavableChatter> loader = getOrCreateConsoleAsync();

        return loader.join();
    }

    public CompletableFuture<SavableChatter> getOrCreateConsoleAsync() {
        String uuid = UserUtils.getConsole().getUuid();

        return CompletableFuture.supplyAsync(() -> {
            Optional<SavableChatter> optional = getKeeper().load(uuid).join();
            if (optional.isPresent()) return optional.get();

            SavableChatter created = instantiate(uuid);
            created.save();

            return created;
        });
    }

    @Override
    public void fireLoadEvents(SavableChatter savableChatter) {

    }

    @Override
    public SavableChatter instantiate(String s) {
        return new SavableChatter(s);
    }

    @Override
    public void fireCreateEvents(SavableChatter savableChatter) {

    }

    public boolean isLoaded(SavableChatter chatter) {
        return isLoaded(chatter.getIdentifier());
    }
}
