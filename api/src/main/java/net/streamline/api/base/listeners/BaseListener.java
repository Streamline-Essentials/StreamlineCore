package net.streamline.api.base.listeners;

import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import singularity.Singularity;
import net.streamline.api.base.module.BaseModule;
import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessageManager;

public class BaseListener implements BaseEventListener {
    public BaseListener() {
        BaseModule.getInstance().logInfo("Loaded " + getClass().getSimpleName());
        BaseEventHandler.bake(this, Singularity.getInstance());
    }

    @BaseProcessor
    public void onProxyMessage(ProxyMessageInEvent event) {
        if (event.getMessage() == null) return;
        if (event.getSubChannel() == null) return;

        ProxiedMessageManager.onProxiedMessageReceived(event.getMessage());
    }
}
