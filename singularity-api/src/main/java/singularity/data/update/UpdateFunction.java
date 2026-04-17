package singularity.data.update;

import singularity.database.servers.UpdateInfo;

import java.util.Optional;
import java.util.function.Function;

/**
 * A functional interface that resolves the most recent {@link UpdateInfo} for a
 * given identifier string.
 *
 * <p>Implementations are used as the <em>parser</em> component of an
 * {@link UpdateType}: they accept a resource identifier and return an
 * {@link Optional} that is present when an update record exists in the database,
 * or empty when no record is found.</p>
 *
 * <p>This interface extends {@link Function}{@code <String, Optional<UpdateInfo>>}
 * so it can be used wherever a standard Java function is expected.</p>
 */
public interface UpdateFunction extends Function<String, Optional<UpdateInfo>> {
}
