package singularity.data.update;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.database.servers.UpdateInfo;

import java.util.Date;
import java.util.Optional;

/**
 * Describes a category of cross-server data that needs to be synchronised between
 * proxy or backend nodes via the shared database.
 *
 * <p>An {@code UpdateType<T>} bundles together:</p>
 * <ul>
 *   <li>A unique {@code identifier} string used as the key in the updates table.</li>
 *   <li>A {@link UpdateFunction} ({@code parser}) that queries the database for the
 *       most recent update record for a given resource identifier.</li>
 *   <li>A {@link PullFunction}{@code <T>} ({@code puller}) that fetches the current
 *       value of the resource from its authoritative source.</li>
 *   <li>A {@link PutterFunction}{@code <T>} ({@code putter}) that applies the freshly
 *       pulled value to its in-memory holder.</li>
 *   <li>A {@code millisBetweenUpdates} threshold used to decide whether a recorded
 *       update is recent enough to act on.</li>
 * </ul>
 *
 * @param <T> the type of the resource being synchronised
 */
@Getter @Setter
public class UpdateType<T> implements Identifiable {

    /**
     * The unique identifier for this update type, used as the key in the updates
     * database table.
     */
    private String identifier;

    /**
     * The Java class representing the resource type {@code T}.
     */
    private Class<T> type;

    /**
     * The function used to query the database for the latest update record for a
     * given resource identifier. Maps an identifier string to an
     * {@link Optional}{@code <UpdateInfo>}.
     */
    private UpdateFunction parser; // identifier -> returns Optional<Date>

    /**
     * The function used to pull the current value of the resource from its
     * authoritative source. Maps an identifier string to an instance of {@code T}.
     */
    private PullFunction<T> puller; // identifier -> returns T

    /**
     * The consumer used to apply a newly pulled resource value to its in-memory
     * representation. Accepts an instance of {@code T} and performs the update.
     */
    private PutterFunction<T> putter; // identifier, T -> void

    /**
     * The minimum number of milliseconds that must have elapsed since the last
     * recorded update before a synchronisation pull is considered necessary.
     */
    private long millisBetweenUpdates;

    /**
     * Creates a fully specified {@code UpdateType} with an explicit parser function.
     *
     * @param identifier          unique key for this update type in the database
     * @param type                the class of the resource being synchronised
     * @param parser              function that retrieves the latest {@link UpdateInfo}
     *                            for a resource identifier
     * @param puller              function that fetches the current resource value
     * @param putter              consumer that applies the fetched value in-memory
     * @param millisBetweenUpdates minimum age in milliseconds before an update is
     *                            considered actionable
     */
    public UpdateType(String identifier, Class<T> type, UpdateFunction parser, PullFunction<T> puller, PutterFunction<T> putter, long millisBetweenUpdates) {
        setIdentifier(identifier);
        setType(type);

        setParser(parser);
        setPuller(puller);
        setPutter(putter);

        setMillisBetweenUpdates(millisBetweenUpdates);
    }

    /**
     * Creates an {@code UpdateType} using the default database-backed parser.
     *
     * @param identifier          unique key for this update type in the database
     * @param type                the class of the resource being synchronised
     * @param puller              function that fetches the current resource value
     * @param putter              consumer that applies the fetched value in-memory
     * @param millisBetweenUpdates minimum age in milliseconds before an update is
     *                            considered actionable
     */
    public UpdateType(String identifier, Class<T> type, PullFunction<T> puller, PutterFunction<T> putter, long millisBetweenUpdates) {
        this(identifier, type, getDefaultParser(identifier), puller, putter, millisBetweenUpdates);
    }

    /**
     * Registers this update type with the {@link UpdateManager}.
     */
    public void load() {
        UpdateManager.load(this);
    }

    /**
     * Removes this update type from the {@link UpdateManager}.
     */
    public void unload() {
        UpdateManager.unload(this);
    }

    /**
     * Returns whether this update type is currently registered with the
     * {@link UpdateManager}.
     *
     * @return {@code true} if registered; {@code false} otherwise
     */
    public boolean isLoaded() {
        return UpdateManager.isLoaded(this);
    }

    /**
     * Asynchronously removes the update record for the given resource identifier
     * from the database, indicating that the update has been consumed.
     *
     * @param identifier the resource identifier whose update record should be cleared
     */
    public void clear(String identifier) {
        Singularity.getMainDatabase().clearUpdateAsync(this, identifier);
    }

    /**
     * Asynchronously posts a new update record for the given resource identifier to
     * the database, signalling to other servers that this resource has changed.
     *
     * @param identifier the resource identifier that was updated
     */
    public void update(String identifier) {
        Singularity.getMainDatabase().postUpdateAsync(this, identifier);
    }

    /**
     * Checks whether a synchronisation pull is needed for the given resource, and if
     * so, pulls the current value, applies it via the {@link #putter}, then clears
     * the pending update record.
     *
     * @param identifier the resource identifier to evaluate
     * @return {@code true} if an update was needed and successfully applied;
     *         {@code false} if no update was required
     */
    public boolean checkAndPut(String identifier) {
        return check(identifier).map(t -> {
            putter.accept(t);
            clear(identifier);
            return true;
        }).orElse(false);
    }

    /**
     * Returns the current resource value if an update is needed for the given
     * identifier, or an empty {@link Optional} if no update is required.
     *
     * @param identifier the resource identifier to evaluate
     * @return an {@link Optional} containing the pulled resource value, or empty if
     *         no synchronisation is needed
     */
    public Optional<T> check(String identifier) {
        if (isUpdateNeeded(identifier)) {
            return Optional.of(pull(identifier));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Fetches the current value of the resource identified by the given string using
     * the configured {@link PullFunction}.
     *
     * @param identifier the resource identifier to pull
     * @return the current resource value; never {@code null}
     */
    public T pull(String identifier) {
        return getPuller().apply(identifier);
    }

    /**
     * Queries the database for the most recent {@link UpdateInfo} associated with the
     * given resource identifier using the configured {@link UpdateFunction} parser.
     *
     * @param identifier the resource identifier to query
     * @return an {@link Optional} containing the latest update info, or empty if none
     *         exists
     */
    public Optional<UpdateInfo> getLastUpdate(String identifier) {
        return getParser().apply(identifier);
    }

    /**
     * Determines whether a synchronisation pull is needed for the given identifier
     * by examining the recorded update timestamp and comparing it against the
     * configured {@link #millisBetweenUpdates} threshold.
     *
     * <p>An update is also considered needed when no record exists in the database,
     * or when the record originated from the current server (updates from this server
     * do not require re-application).</p>
     *
     * @param identifier the resource identifier to check
     * @return {@code true} if a pull should be performed; {@code false} otherwise
     */
    public boolean isUpdateNeeded(String identifier) {
        return getLastUpdate(identifier).map(info -> {
            if (info.getServerUuid().equals(GivenConfigs.getServer().getUuid())) return false;

            return isUpdateNeeded(info.getDate(), millisBetweenUpdates);
        }).orElse(true);
    }

    /**
     * Returns an {@link UpdateFunction} that queries the main database for the latest
     * update record for this update type and the given resource identifier.
     *
     * <p>The returned function ignores the string argument it receives and always uses
     * the bound {@code identifier} parameter instead.</p>
     *
     * @param identifier the resource identifier the returned parser will query
     * @return a database-backed {@link UpdateFunction}
     */
    public UpdateFunction defaultParser(String identifier) {
        return (s) -> Singularity.getMainDatabase().checkUpdate(this, identifier).join();
    }

    /**
     * Adjusts the given time in milliseconds, ensuring that the
     * {@code millis + millisBetweenUpdates - adjusted amount} is always less than
     * {@link System#currentTimeMillis()} when the method is called
     * {@code millisBetweenUpdates} after the last update.
     *
     * @param millis The time in milliseconds to adjust.
     * @return The adjusted time in milliseconds.
     */
    public static long getAdjustedMillis(long millis) {
        return millis - 1; // will be 1 millisecond less than the current millis.
    }

    /**
     * Returns whether sufficient time has elapsed since the given epoch timestamp for
     * a synchronisation update to be considered necessary.
     *
     * @param millis               the epoch timestamp (in milliseconds) of the last
     *                             recorded update
     * @param millisBetweenUpdates the minimum number of milliseconds that must have
     *                             passed before an update is actionable
     * @return {@code true} if an update should be performed; {@code false} otherwise
     */
    public static boolean isUpdateNeeded(long millis, long millisBetweenUpdates) {
        return getAdjustedMillis(millis) + millisBetweenUpdates < System.currentTimeMillis();
    }

    /**
     * Returns whether sufficient time has elapsed since the given {@link Date} for
     * a synchronisation update to be considered necessary.
     *
     * @param date                 the date of the last recorded update
     * @param millisBetweenUpdates the minimum number of milliseconds that must have
     *                             passed before an update is actionable
     * @return {@code true} if an update should be performed; {@code false} otherwise
     */
    public static boolean isUpdateNeeded(Date date, long millisBetweenUpdates) {
        return isUpdateNeeded(date.getTime(), millisBetweenUpdates);
    }

    /**
     * Creates a default {@link UpdateFunction} backed by the {@link UpdateManager}
     * for the given update type identifier. The returned function delegates to
     * {@link UpdateManager#getLastUpdate(String, String)}.
     *
     * @param updateType the identifier of the update type to query
     * @return a default database-backed {@link UpdateFunction}
     */
    public static UpdateFunction getDefaultParser(String updateType) {
        return (s) -> UpdateManager.getLastUpdate(updateType, s);
    }
}
