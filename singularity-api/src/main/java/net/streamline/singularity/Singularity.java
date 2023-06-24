package net.streamline.singularity;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.base.listeners.BaseListener;
import net.streamline.api.base.module.BaseModule;
import net.streamline.api.base.ratapi.StreamlineExpansion;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.SimpleModule;
import tv.quaint.thebase.lib.pf4j.PluginDependency;
import tv.quaint.thebase.lib.pf4j.PluginDescriptor;
import tv.quaint.thebase.lib.pf4j.PluginWrapper;

import java.util.Collections;
import java.util.List;

public class Singularity extends SimpleModule {
    @Getter
    private static Singularity instance;

    @Getter @Setter
    private static BaseListener baseListener;

    @Getter @Setter
    private static StreamlineExpansion streamlineExpansion;

    public Singularity() {
        super(new PluginWrapper(ModuleManager.safePluginManager(), new PluginDescriptor() {
            @Override
            public String getPluginId() {
                return "singularity";
            }

            @Override
            public String getPluginDescription() {
                return "Singularity module.";
            }

            @Override
            public String getPluginClass() {
                return "net.streamline.singularity.Singularity";
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
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}