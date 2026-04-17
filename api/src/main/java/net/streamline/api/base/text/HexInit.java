package net.streamline.api.base.text;

import net.streamline.api.base.module.BaseModule;
import singularity.modules.ModuleUtils;
import singularity.text.HexPolicy;
import singularity.text.TextManager;

/**
 * Utility class that registers the built-in hex-colour parsing policies with
 * the Streamline {@link singularity.text.TextManager}.
 *
 * <p>Three formats are supported after initialisation:
 * <ul>
 *   <li>{@code {#RRGGBB}} — brace-delimited hex codes.</li>
 *   <li>{@code <#RRGGBB>} — angle-bracket-delimited hex codes.</li>
 *   <li>{@code &#RRGGBB} — ampersand-prefixed hex codes (no closing delimiter).</li>
 * </ul>
 * This class is not intended to be instantiated.
 */
public class HexInit {

    /**
     * Registers the three standard hex-colour {@link singularity.text.HexPolicy}
     * instances with {@link singularity.text.TextManager} and logs the total
     * number of registered policies.
     */
    public static void init() {
        TextManager.registerHexPolicy(new HexPolicy("{#", "}"));
        TextManager.registerHexPolicy(new HexPolicy("<#", ">"));
        TextManager.registerHexPolicy(new HexPolicy("&#", ""));

        ModuleUtils.logInfo(BaseModule.getInstance(), "Registered " + TextManager.getHexPolicies().size() + " hex policies.");
    }
}
