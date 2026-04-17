package net.streamline.apib.depends;

import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.streamline.apib.SLAPIB;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.players.CosmicPlayer;
import singularity.holders.CosmicDependencyHolder;
import singularity.modules.ModuleUtils;
import singularity.utils.MessageUtils;

/**
 * Dependency holder that integrates Streamline with PlaceholderAPI (PAPI) on
 * Spigot/Bukkit servers. When PAPI is detected the holder registers a
 * {@link StreamlinePAPIExpansion} that exposes Streamline placeholders under
 * the {@code %streamline_<param>%} namespace.
 */
@Setter
@Getter
public class PAPIDepend extends CosmicDependencyHolder<PlaceholderAPI> {

    /**
     * The registered PlaceholderAPI expansion, or {@code null} when PAPI is
     * not present.
     */
    private StreamlinePAPIExpansion papiExpansion;

    /**
     * Constructs a {@code PAPIDepend} holder and, if PlaceholderAPI is
     * present on the server, registers the {@link StreamlinePAPIExpansion}.
     * Logs a warning when PAPI is absent and support is disabled.
     */
    public PAPIDepend() {
        super("PlaceholderAPI", "PlaceholderAPI", "papi", "placeholderapi");
        if (isPresent()) {
            setPapiExpansion(new StreamlinePAPIExpansion());
        } else {
            MessageUtils.logInfo("Could not find PlaceholderAPI... Disabling support...!");
        }
    }

    /**
     * PlaceholderAPI expansion that exposes Streamline-managed placeholders
     * to other plugins. All placeholders are served under the identifier
     * {@code streamline}. A leading {@code !} in the parameter string causes
     * the parameter to be treated as an arbitrary placeholder rather than a
     * Streamline-namespaced one.
     */
    public static class StreamlinePAPIExpansion extends PlaceholderExpansion {

        /**
         * Constructs and immediately registers this expansion with PlaceholderAPI.
         * Logs a confirmation message on success.
         */
        public StreamlinePAPIExpansion() {
            if (register()) MessageUtils.logInfo("Streamline PAPI Expansion loaded!");
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code "streamline"}
         */
        @Override
        public @NotNull String getIdentifier() {
            return "streamline";
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns {@code true} so the expansion survives PlaceholderAPI reloads
         * without needing to be re-registered.
         *
         * @return {@code true}
         */
        @Override
        public boolean persist() {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@code "Drak"}
         */
        @Override
        public @NotNull String getAuthor() {
            return "Drak";
        }

        /**
         * {@inheritDoc}
         * <p>
         * Delegates to the Bukkit plugin description to retrieve the currently
         * running plugin version.
         *
         * @return the version string from the Streamline-BAPI plugin descriptor
         */
        @Override
        public @NotNull String getVersion() {
            return SLAPIB.getPlugin().getDescription().getVersion();
        }

        /**
         * {@inheritDoc}
         *
         * @return the result of the parent {@link PlaceholderExpansion#register()} call
         */
        @Override
        public boolean register() {
            return super.register();
        }

        /**
         * {@inheritDoc}
         * <p>
         * Resolves the requesting offline player to a {@link CosmicPlayer} and
         * passes the constructed placeholder string through the Streamline
         * placeholder engine. If the parameter begins with {@code !} it is
         * treated as a raw placeholder (wrapped in {@code %…%}); otherwise it
         * is prefixed with {@code %streamline_} automatically.
         *
         * @param player the offline player requesting the placeholder
         * @param params the placeholder parameter string (without surrounding {@code %%})
         * @return the resolved placeholder value, or the configured null-placeholder
         *         string when the player cannot be resolved
         */
        @Override
        public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
            CosmicPlayer streamPlayer = ModuleUtils.getOrCreatePlayer(player.getUniqueId().toString()).orElse(null);
            if (streamPlayer == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
            String toParse;
            if (params.startsWith("!")) toParse = "%" + params.substring("!".length()) + "%";
            else toParse = "%streamline_" + params + "%";
            return ModuleUtils.replaceAllPlayerBungee(streamPlayer, toParse);
        }
    }
}