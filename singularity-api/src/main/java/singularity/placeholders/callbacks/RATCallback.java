package singularity.placeholders.callbacks;

/**
 * Marker interface for all placeholder callback types in the RAT
 * (Replace-And-Transform) placeholder system.
 *
 * <p>All concrete callback interfaces ({@link PlaceholderCallback},
 * {@link UserPlaceholderCallback}) extend this interface so that
 * {@link singularity.placeholders.replaceables.AbstractReplaceable} can
 * hold a single typed reference to any callback variant.
 */
public interface RATCallback {
}
