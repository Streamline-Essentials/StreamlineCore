package host.plas.database;

import host.plas.StreamlineGroups;
import host.plas.data.player.GroupedPlayer;
import singularity.data.console.CosmicSender;
import singularity.database.modules.DBKeeper;
import singularity.loading.Loader;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PlayerLoader extends Loader<GroupedPlayer> {
    /**
     * Returns the module's loader.
     *
     * <p>Resolves through {@link StreamlineGroups} rather than holding a second static
     * instance, so that every caller shares the one in-memory set of loaded players.</p>
     */
    public static PlayerLoader getInstance() {
        PlayerLoader loader = StreamlineGroups.getPlayerLoader();
        if (loader == null) {
            // Called before onEnable finished wiring the module up.
            loader = new PlayerLoader();
            StreamlineGroups.setPlayerLoader(loader);
        }

        return loader;
    }

    @Override
    public DBKeeper<GroupedPlayer> getKeeper() {
        return StreamlineGroups.getPlayerKeeper();
    }

    @Override
    public GroupedPlayer getConsole() {
        CosmicSender console = UserUtils.getConsole();
        Optional<GroupedPlayer> optional = getLoaded().stream().filter(a -> a.getIdentifier().equals(console.getUuid())).findFirst();
        if (optional.isPresent()) return optional.get();

        CompletableFuture<GroupedPlayer> loader = getOrCreateConsoleAsync();

        return loader.join();
    }

    public CompletableFuture<GroupedPlayer> getOrCreateConsoleAsync() {
        String uuid = UserUtils.getConsole().getUuid();

        return CompletableFuture.supplyAsync(() -> {
            Optional<GroupedPlayer> optional = getKeeper().load(uuid).join();
            if (optional.isPresent()) return optional.get();

            GroupedPlayer created = instantiate(uuid);
            created.save();

            return created;
        });
    }

    @Override
    public void fireLoadEvents(GroupedPlayer savableChatter) {

    }

    @Override
    public GroupedPlayer instantiate(String s) {
        return new GroupedPlayer(s);
    }

    @Override
    public void fireCreateEvents(GroupedPlayer savableChatter) {

    }

    public boolean isLoaded(GroupedPlayer chatter) {
        return isLoaded(chatter.getIdentifier());
    }
}
