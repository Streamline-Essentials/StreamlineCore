package net.streamline.apib.depends;

import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.holders.StreamlineDependencyHolder;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.utils.MessageUtils;
import net.streamline.apib.SLAPIB;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class PAPIDepend extends StreamlineDependencyHolder<PlaceholderAPI> {
    @Setter
    private StreamlinePAPIExpansion papiExpansion;

    public PAPIDepend() {
        super("PlaceholderAPI", "PlaceholderAPI", "papi", "placeholderapi");
        if (isPresent()) {
            tryLoad(() -> {
                setApi(null);
                setPapiExpansion(new StreamlinePAPIExpansion());
                return null;
            });
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
        public @NotNull String getAuthor() {
            return "Quaint";
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
            StreamlinePlayer streamlinePlayer = ModuleUtils.getOrGetPlayer(player.getUniqueId().toString());
            if (streamlinePlayer == null) return MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_NULL.get();
            String toParse;
            if (params.startsWith("!")) toParse = "%" + params.substring("!".length()) + "%";
            else toParse = "%streamline_" + params + "%";
            return ModuleUtils.replaceAllPlayerBungee(streamlinePlayer, toParse);
        }
    }
}