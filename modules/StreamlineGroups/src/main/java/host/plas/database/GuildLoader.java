package host.plas.database;

import host.plas.StreamlineGroups;
import host.plas.data.Guild;
import singularity.database.modules.DBKeeper;
import singularity.loading.Loader;

import java.util.Optional;

/**
 * Holds the guilds that are currently in memory and backs them with {@link GuildKeeper}.
 *
 * <p>Unlike players, guilds are not per-sender, so there is no console guild; the console
 * is not a member of anything and {@link #getConsole()} is unsupported.</p>
 */
public class GuildLoader extends Loader<Guild> {
    /**
     * Returns the module's guild loader, resolved through {@link StreamlineGroups} so that
     * every caller shares one in-memory set.
     */
    public static GuildLoader getInstance() {
        GuildLoader loader = StreamlineGroups.getGuildLoader();
        if (loader == null) {
            loader = new GuildLoader();
            StreamlineGroups.setGuildLoader(loader);
        }

        return loader;
    }

    @Override
    public DBKeeper<Guild> getKeeper() {
        return StreamlineGroups.getGuildKeeper();
    }

    /**
     * Guilds have no console equivalent.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public Guild getConsole() {
        throw new UnsupportedOperationException("The console does not belong to a guild.");
    }

    /**
     * Looks a guild up by uuid.
     *
     * <p>Overridden so that the base class's console short-circuit -- which would call the
     * unsupported {@link #getConsole()} -- never runs for guilds.</p>
     */
    @Override
    public Optional<Guild> get(String identifier) {
        if (identifier == null) return Optional.empty();

        return getLoaded().stream().filter(a -> a.getIdentifier().equals(identifier)).findFirst();
    }

    @Override
    public Guild instantiate(String identifier) {
        // Built without loading so that instantiating does not re-enter the group manager
        // while the loader is still deciding what to do with it.
        return new Guild(identifier, false);
    }

    @Override
    public void fireLoadEvents(Guild loaded) {

    }

    @Override
    public void fireCreateEvents(Guild created) {

    }

    public boolean isLoaded(Guild guild) {
        return isLoaded(guild.getIdentifier());
    }
}
