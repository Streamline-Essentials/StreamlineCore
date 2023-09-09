package net.streamline.api.base.module;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.base.ratapi.StreamlineExpansion;
import net.streamline.api.base.text.HexInit;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.SimpleModule;
import net.streamline.api.base.listeners.BaseListener;
import net.streamline.api.text.TextManager;
import tv.quaint.thebase.lib.pf4j.PluginDependency;
import tv.quaint.thebase.lib.pf4j.PluginDescriptor;
import tv.quaint.thebase.lib.pf4j.PluginWrapper;

import java.util.Collections;
import java.util.List;

public class BaseModule extends SimpleModule {
    @Getter
    private static BaseModule instance;

    @Getter @Setter
    private static BaseListener baseListener;

    @Getter @Setter
    private static StreamlineExpansion streamlineExpansion;

    public BaseModule() {
        super(new PluginWrapper(ModuleManager.safePluginManager(), new PluginDescriptor() {
            @Override
            public String getPluginId() {
                return "streamline-base";
            }

            @Override
            public String getPluginDescription() {
                return "Base module.";
            }

            @Override
            public String getPluginClass() {
                return "net.streamline.api.base.module.BaseModule";
            }

            @Override
            public String getVersion() {
                return "0.0.1";
            }

            @Override
            public String getRequires() {
                return "";
            }

            @Override
            public String getProvider() {
                return "";
            }

            @Override
            public String getLicense() {
                return "";
            }

            @Override
            public List<PluginDependency> getDependencies() {
                return Collections.emptyList();
            }
        }, SLAPI.getModuleFolder().toPath(), SLAPI.getInstance().getClass().getClassLoader()));

        setMalleable(false);
        setEnabled(true);
        ModuleManager.getEnabledModules().put(getIdentifier(), this);
    }

    @Override
    public void onLoad() {
        instance = this;
        setBaseListener(new BaseListener());
//        ModuleUtils.listen(getBaseListener(), this);

        setStreamlineExpansion(new StreamlineExpansion());

//        GivenConfigs.getMainConfig().getHexPolicies().forEach(TextManager::registerHexPolicy);
        HexInit.init();
    }

    @Override
    public void onEnable() {
        // nothing right now.
        getStreamlineExpansion().init();
    }

    @Override
    public void onDisable() {
        // nothing right now.
        getStreamlineExpansion().stop();
    }
}
