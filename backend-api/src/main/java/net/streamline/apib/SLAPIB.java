package net.streamline.apib;

import host.plas.bou.utils.UuidUtils;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.apib.depends.PAPIDepend;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;

import java.util.Optional;
import java.util.UUID;

/**
 * Bukkit/Spigot-specific API entry point for the Streamline backend layer.
 *
 * <p>{@code SLAPIB} (Streamline API Backend) holds the global singleton
 * references for the Bukkit platform plugin, the core {@link SLAPI} instance,
 * and the PlaceholderAPI integration. It also provides convenience methods for
 * resolving Streamline user objects to their native Bukkit {@link Player} and
 * {@link OfflinePlayer} counterparts.
 */
public class SLAPIB {

    /** The currently active {@code SLAPIB} instance. */
    @Getter @Setter
    private static SLAPIB instance;

    /** The core Streamline API instance tied to this Bukkit platform. */
    @Getter @Setter
    private static SLAPI<?, ?, ?, ?, ?> slapi;

    /** The owning Bukkit plugin instance (typically the Streamline Spigot module). */
    @Getter @Setter
    private static Plugin plugin;

    /** The PlaceholderAPI integration holder; {@code null} when PAPI is absent. */
    @Getter @Setter
    private static PAPIDepend papiDepend;

    /**
     * Constructs a new {@code SLAPIB}, registers it as the global singleton,
     * and initialises the PlaceholderAPI integration.
     *
     * @param slapi  the core {@link SLAPI} instance for this server
     * @param plugin the Bukkit plugin that owns this instance
     */
    public SLAPIB(SLAPI<?, ?, ?, ?, ?> slapi, JavaPlugin plugin) {
        instance = this;
        setPlugin(plugin);
        setSlapi(slapi);

        setPapiDepend(new PAPIDepend());
    }

    /**
     * Attempts to resolve the given UUID string to an online Bukkit
     * {@link Player}.
     *
     * @param uuid the player's UUID string
     * @return an {@link Optional} containing the online {@link Player}, or
     *         empty if the UUID is invalid or the player is offline
     */
    public static Optional<Player> asPlayer(String uuid) {
        if (! UuidUtils.isValidPlayerUUID(uuid)) return Optional.empty();

        return Optional.ofNullable(Bukkit.getPlayer(UUID.fromString(uuid)));
    }

    /**
     * Attempts to resolve the given UUID string to a Bukkit
     * {@link OfflinePlayer}.
     *
     * @param uuid the player's UUID string
     * @return an {@link Optional} containing the {@link OfflinePlayer}, or
     *         empty if the UUID is invalid
     */
    public static Optional<OfflinePlayer> asOfflinePlayer(String uuid) {
        if (! UuidUtils.isValidPlayerUUID(uuid)) return Optional.empty();

        return Optional.of(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
    }

    /**
     * Resolves a {@link CosmicPlayer} to an online Bukkit {@link Player}.
     *
     * @param player the Streamline player to resolve
     * @return an {@link Optional} containing the online {@link Player}, or
     *         empty if the player is offline
     */
    public static Optional<Player> asPlayer(CosmicPlayer player) {
        return asPlayer(player.getUuid());
    }

    /**
     * Resolves a {@link CosmicPlayer} to a Bukkit {@link OfflinePlayer}.
     *
     * @param player the Streamline player to resolve
     * @return an {@link Optional} containing the {@link OfflinePlayer}
     */
    public static Optional<OfflinePlayer> asOfflinePlayer(CosmicPlayer player) {
        return asOfflinePlayer(player.getUuid());
    }

    /**
     * Resolves a {@link CosmicSender} to an online Bukkit {@link Player}.
     * Non-player senders (e.g. the console) will always yield an empty
     * optional because their UUIDs are not valid player UUIDs.
     *
     * @param player the Streamline sender to resolve
     * @return an {@link Optional} containing the online {@link Player}, or
     *         empty if the sender is not an online player
     */
    public static Optional<Player> asPlayer(CosmicSender player) {
        return asPlayer(player.getUuid());
    }

    /**
     * Resolves a {@link CosmicSender} to a Bukkit {@link OfflinePlayer}.
     *
     * @param player the Streamline sender to resolve
     * @return an {@link Optional} containing the {@link OfflinePlayer}, or
     *         empty if the UUID is not a valid player UUID
     */
    public static Optional<OfflinePlayer> asOfflinePlayer(CosmicSender player) {
        return asOfflinePlayer(player.getUuid());
    }
}
