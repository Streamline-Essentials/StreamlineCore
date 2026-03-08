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

public class SLAPIB {
    @Getter @Setter
    private static SLAPIB instance;
    @Getter @Setter
    private static SLAPI<?, ?, ?, ?, ?> slapi;
    @Getter @Setter
    private static Plugin plugin;
    @Getter @Setter
    private static PAPIDepend papiDepend;

    public SLAPIB(SLAPI<?, ?, ?, ?, ?> slapi, JavaPlugin plugin) {
        instance = this;
        setPlugin(plugin);
        setSlapi(slapi);

        setPapiDepend(new PAPIDepend());
    }

    public static Optional<Player> asPlayer(String uuid) {
        if (! UuidUtils.isValidPlayerUUID(uuid)) return Optional.empty();

        return Optional.ofNullable(Bukkit.getPlayer(UUID.fromString(uuid)));
    }

    public static Optional<OfflinePlayer> asOfflinePlayer(String uuid) {
        if (! UuidUtils.isValidPlayerUUID(uuid)) return Optional.empty();

        return Optional.of(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
    }

    public static Optional<Player> asPlayer(CosmicPlayer player) {
        return asPlayer(player.getUuid());
    }

    public static Optional<OfflinePlayer> asOfflinePlayer(CosmicPlayer player) {
        return asOfflinePlayer(player.getUuid());
    }

    public static Optional<Player> asPlayer(CosmicSender player) {
        return asPlayer(player.getUuid());
    }

    public static Optional<OfflinePlayer> asOfflinePlayer(CosmicSender player) {
        return asOfflinePlayer(player.getUuid());
    }
}
