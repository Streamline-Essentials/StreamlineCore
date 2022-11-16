package net.streamline.api.base.module;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.base.ratapi.StreamlineExpansion;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.modules.SimpleModule;
import net.streamline.api.modules.dependencies.Dependency;
import net.streamline.api.base.listeners.BaseListener;
import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginWrapper;

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
    }

    @Override
    public void onLoad() {
        instance = this;
        setBaseListener(new BaseListener());
//        ModuleUtils.listen(getBaseListener(), this);

        setStreamlineExpansion(new StreamlineExpansion());
    }

    @Override
    public void onEnable() {
        // nothing right now.
        getStreamlineExpansion().register();
    }

    @Override
    public void onDisable() {
        // nothing right now.
        getStreamlineExpansion().unregister();
    }

    @Override
    public String getIdentifier() {
        return identifier();
    }
}
