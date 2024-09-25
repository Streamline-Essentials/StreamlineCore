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

@Setter
@Getter
public class PAPIDepend extends CosmicDependencyHolder<PlaceholderAPI> {
    private StreamlinePAPIExpansion papiExpansion;

    public PAPIDepend() {
        super("PlaceholderAPI", "PlaceholderAPI", "papi", "placeholderapi");
        if (isPresent()) {
            setPapiExpansion(new StreamlinePAPIExpansion());
        } else {
            MessageUtils.logInfo("Could not find PlaceholderAPI... Disabling support...!");
        }
    }

    public static class StreamlinePAPIExpansion extends PlaceholderExpansion {
        public StreamlinePAPIExpansion() {
            if (register()) MessageUtils.logInfo("Streamline PAPI Expansion loaded!");
        }

        @Override
        public @NotNull String getIdentifier() {
            return "streamline";
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public @NotNull String getAuthor() {
            return "Drak";
        }

        @Override
        public @NotNull String getVersion() {
            return SLAPIB.getPlugin().getDescription().getVersion();
        }

        @Override
        public boolean register() {
            return super.register();
        }

        @Override
        public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
            CosmicPlayer streamPlayer = ModuleUtils.getOrCreatePlayer(player.getUniqueId().toString());
            if (streamPlayer == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
            String toParse;
            if (params.startsWith("!")) toParse = "%" + params.substring("!".length()) + "%";
            else toParse = "%streamline_" + params + "%";
            return ModuleUtils.replaceAllPlayerBungee(streamPlayer, toParse);
        }
    }
}