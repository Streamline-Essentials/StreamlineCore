package host.plas.adapters.placeholderapi;

import net.streamline.api.SLAPI;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

public class PlaceholderAdapter {
    public static boolean isAvailable() {
        return ! SLAPI.isProxy();
    }

    public static boolean isEnabled() {
        return isAvailable() && PlaceholderAccessor.isEnabled();
    }

    public static String replace(String input) {
        return ModuleUtils.replacePlaceholders(input);

//        if (! isEnabled()) return ModuleUtils.replacePlaceholders(input);
//        return PlaceholderAccessor.replace(input);
    }

    public static String replace(CosmicSender sender, String input) {
        return ModuleUtils.replacePlaceholders(sender, input);

//        if (! isEnabled()) return ModuleUtils.replacePlaceholders(sender, input);
//        return PlaceholderAccessor.replace(sender, input);
    }


    public static String replace(String uuid, String input) {
        CosmicSender sender = UserUtils.getOrGetSender(uuid).orElse(null);
        if (sender == null) return replace(input);

        return replace(sender, input);
    }
}
