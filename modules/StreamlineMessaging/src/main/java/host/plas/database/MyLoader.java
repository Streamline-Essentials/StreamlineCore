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
            SavableChatter console;
            if (optional.isPresent()) {
                console = optional.get();
            } else {
                console = instantiate(uuid);
                console.save();
            }
            console.setFullyLoaded(true);

            // Added directly: load(...) resolves the console identifier back through
            // getConsole(), which would recurse. Holding it keeps later lookups from
            // re-reading the row and its changes from being lost.
            getLoaded().add(console);
            console.startStoredInvites();

            return console;
        });
    }

    @Override
    public void fireLoadEvents(SavableChatter savableChatter) {
        savableChatter.startStoredInvites();
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
