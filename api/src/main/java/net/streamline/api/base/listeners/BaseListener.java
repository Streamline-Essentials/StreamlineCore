package net.streamline.api.base.listeners;

import singularity.Singularity;
import net.streamline.api.base.module.BaseModule;
import singularity.messages.events.ProxyMessageInEvent;
import singularity.messages.proxied.ProxiedMessageManager;
import tv.quaint.events.BaseEventHandler;
import tv.quaint.events.BaseEventListener;
import tv.quaint.events.processing.BaseProcessor;

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
