package net.streamline.api.base.text;

import net.streamline.api.base.module.BaseModule;
import singularity.modules.ModuleUtils;
import singularity.text.HexPolicy;
import singularity.text.TextManager;

public class HexInit {
    public static void init() {
        TextManager.registerHexPolicy(new HexPolicy("{#", "}"));
        TextManager.registerHexPolicy(new HexPolicy("<#", ">"));
        TextManager.registerHexPolicy(new HexPolicy("&#", ""));

        ModuleUtils.logInfo(BaseModule.getInstance(), "Registered " + TextManager.getHexPolicies().size() + " hex policies.");
    }
}
