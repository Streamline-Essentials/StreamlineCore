package net.streamline.platform.listeners;

import gg.drak.thebase.async.AsyncTask;
import gg.drak.thebase.async.AsyncUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.streamline.client.ModuleKeys;
import net.streamline.client.StreamlineClient;
import net.streamline.client.compat.ClientCompat;
import net.streamline.client.compat.GfxCanvas;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.commands.ClientCommand;
import org.lwjgl.input.Keyboard;
import singularity.client.ClientBridge;

import java.util.ArrayList;

/**
 * Hooks Streamline into the 1.8.9 client: boots it on the first client tick, runs queued
 * screen switches, draws the HUD, registers {@code /streamlineclient} and the HUD Editor
 * (Right Shift) and StreamlineClient Menu (unbound) key bindings, and takes Streamline down
 * when the game exits.
 */
public final class LegacyClientListener {

    private static final String KEY_CATEGORY = "key.category.streamlinecore.streamlineclient";
    private static final KeyBinding HUD_EDITOR = new KeyBinding("key.streamlinecore.hud_editor", Keyboard.KEY_RSHIFT, KEY_CATEGORY);
    private static final KeyBinding MENU = new KeyBinding("key.streamlinecore.menu", Keyboard.KEY_NONE, KEY_CATEGORY);

    private static boolean booted;

    private LegacyClientListener() {}

    public static void register() {
        // Installed before boot, so modules find the client handler as soon as they start.
        StreamlineClient.init();
        MinecraftForge.EVENT_BUS.register(new LegacyClientListener());
        ClientCommandHandler.instance.registerCommand(new ClientCommand());
        ClientRegistry.registerKeyBinding(HUD_EDITOR);
        ClientRegistry.registerKeyBinding(MENU);
        // 1.8.9 Forge fires no event as the game exits; the JVM's shutdown is the last chance.
        Runtime.getRuntime().addShutdownHook(new Thread(LegacyClientListener::shutdown, "Streamline shutdown"));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (! booted) {
            booted = true;
            try {
                BasePlugin.getInstance().boot();
            } catch (Throwable t) {
                BasePlugin.warn("Streamline could not start on the client", t);
            }
        }
        ClientCompat.runQueued();
        ModuleKeys.tick();
        keys();
    }

    /** Opens the screen of a key pressed since the last tick; presses count only with no screen open. */
    private static void keys() {
        boolean editor = drain(HUD_EDITOR);
        boolean menu = drain(MENU);
        if (Minecraft.getMinecraft().currentScreen != null) return;
        if (editor) ClientBridge.openHudEditor();
        else if (menu) ClientBridge.openMenu();
    }

    private static boolean drain(KeyBinding binding) {
        boolean pressed = false;
        while (binding.isPressed()) pressed = true;
        return pressed;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        StreamlineClient.renderHud(GfxCanvas.wrap(event.resolution));
    }

    private static void shutdown() {
        BasePlugin plugin = BasePlugin.getInstance();
        if (plugin == null) return;
        try {
            plugin.shutdownStreamline();
        } catch (Throwable t) {
            BasePlugin.log("Streamline shutdown failed: " + t);
        }
        // TheBase runs its tasks on Swing timers, whose thread would keep the JVM alive.
        for (AsyncTask task : new ArrayList<>(AsyncUtils.getQueuedTasks())) {
            task.stop();
        }
        AsyncUtils.getQueuedTasks().clear();
    }
}
