package singularity.data.update;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.database.servers.UpdateInfo;
import tv.quaint.objects.Identifiable;

import java.util.Date;
import java.util.Optional;

@Getter @Setter
public class UpdateType<T> implements Identifiable {
    private String identifier;
    private Class<T> type;
    private UpdateFunction parser; // identifier -> returns Optional<Date>
    private PullFunction<T> puller; // identifier -> returns T
    private PutterFunction<T> putter; // identifier, T -> void
    private long millisBetweenUpdates;

    public UpdateType(String identifier, Class<T> type, UpdateFunction parser, PullFunction<T> puller, PutterFunction<T> putter, long millisBetweenUpdates) {
        setIdentifier(identifier);
        setType(type);

        setParser(parser);
        setPuller(puller);
        setPutter(putter);

        setMillisBetweenUpdates(millisBetweenUpdates);
    }

    public UpdateType(String identifier, Class<T> type, PullFunction<T> puller, PutterFunction<T> putter, long millisBetweenUpdates) {
        this(identifier, type, getDefaultParser(identifier), puller, putter, millisBetweenUpdates);
    }

    public void load() {
        UpdateManager.load(this);
    }

    public void unload() {
        UpdateManager.unload(this);
    }

    public boolean isLoaded() {
        return UpdateManager.isLoaded(this);
    }

    public void clear(String identifier) {
        Singularity.getMainDatabase().clearUpdateAsync(this, identifier);
    }

    public void update(String identifier) {
        Singularity.getMainDatabase().postUpdateAsync(this, identifier);
    }

    public boolean checkAndPut(String identifier) {
        return check(identifier).map(t -> {
            putter.accept(t);
            clear(identifier);
            return true;
        }).orElse(false);
    }

    public Optional<T> check(String identifier) {
        if (isUpdateNeeded(identifier)) {
            return Optional.of(pull(identifier));
        } else {
            return Optional.empty();
        }
    }

    public T pull(String identifier) {
        return getPuller().apply(identifier);
    }

    public Optional<UpdateInfo> getLastUpdate(String identifier) {
        return getParser().apply(identifier);
    }

    public boolean isUpdateNeeded(String identifier) {
        return getLastUpdate(identifier).map(info -> {
            if (info.getServerUuid().equals(GivenConfigs.getServer().getUuid())) return false;

            return isUpdateNeeded(info.getDate(), millisBetweenUpdates);
        }).orElse(true);
    }

    public UpdateFunction defaultParser(String identifier) {
        return (s) -> Singularity.getMainDatabase().checkUpdate(this, identifier).join();
    }

    /**
     * Adjusts the given time in milliseconds, ensuring that the `millis + millisBetweenUpdates - adjusted amount`
     * is always less than @link{System#currentTimeMillis()} when the method is called @link{millisBetweenUpdates}
     * after the last update.
     * For example,
     * given getAdjustedMillis(date.getTime()) + getMillisBetweenUpdates() < System.currentTimeMillis(),
     * if date.getTime() = 1000, getMillisBetweenUpdates() = 1000, System.currentTimeMillis() = 2000,
     * it will return 1000 - 1 = 999
     * if date.getTime() = 1010, getMillisBetweenUpdates() = 1000, System.currentTimeMillis() = 2000,
     * it will return 1001 - 1 = 1000
     *
     * @param millis The time in milliseconds to adjust.
     * @return The adjusted time in milliseconds.
     */
    public static long getAdjustedMillis(long millis) {
        return millis - 1; // will be 1 millisecond less than the current millis.
    }

    public static boolean isUpdateNeeded(long millis, long millisBetweenUpdates) {
        return getAdjustedMillis(millis) + millisBetweenUpdates < System.currentTimeMillis();
    }

    public static boolean isUpdateNeeded(Date date, long millisBetweenUpdates) {
        return isUpdateNeeded(date.getTime(), millisBetweenUpdates);
    }

    public static UpdateFunction getDefaultParser(String updateType) {
        return (s) -> UpdateManager.getLastUpdate(updateType, s);
    }
}
