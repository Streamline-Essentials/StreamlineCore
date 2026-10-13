package net.streamline.client;

import net.streamline.client.compat.ClientCompat;
import singularity.client.ClientEnvironment;
import singularity.client.input.ClientInput;
import singularity.client.settings.ClientSettingsRegistry;
import singularity.client.settings.ModuleSettings;
import singularity.client.settings.Setting;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Runs client modules' key settings ({@link Setting.Key}) and keeps the game keys they hold
 * ({@link ClientInput}) pressed. Each loader's client tick calls {@link #tick()}.
 *
 * <p>A key setting fires when its key goes down while no screen is open. Keys are read from
 * the window rather than from key events, since modules load long after the game has handed
 * out its key mappings.</p>
 */
public final class ModuleKeys {

    /** Whether each key setting's key was down last tick; weak, so a stopped module's settings go. */
    private static final Map<Setting.Key, Boolean> WAS_DOWN = new WeakHashMap<Setting.Key, Boolean>();
    /** Game keys pressed for a module last tick, released once no module holds them. */
    private static final Set<ClientInput.GameKey> APPLIED = EnumSet.noneOf(ClientInput.GameKey.class);

    private ModuleKeys() {}

    public static void tick() {
        boolean inGame = ClientCompat.currentScreen() == null;
        for (ModuleSettings page : ClientSettingsRegistry.all()) {
            for (Setting<?> setting : page.getSettings()) {
                if (setting instanceof Setting.Key) poll((Setting.Key) setting, inGame);
            }
        }
        for (ClientInput.GameKey key : ClientInput.GameKey.values()) {
            if (ClientInput.isHeld(key)) {
                // Pressed again every tick: the game lets go of keys when a screen opens or the
                // physical key is released.
                ClientCompat.holdGameKey(key, true);
                APPLIED.add(key);
            } else if (APPLIED.remove(key)) {
                ClientCompat.holdGameKey(key, false);
            }
        }
    }

    private static void poll(Setting.Key setting, boolean inGame) {
        boolean down = setting.isBound() && ClientCompat.keyDown(setting.getValue());
        Boolean was = WAS_DOWN.put(setting, down);
        // A key already held when its setting appeared, or when a screen closed, does not fire.
        if (! down || was == null || was || ! inGame) return;
        Runnable action = setting.getOnPress();
        if (action == null) return;
        try {
            action.run();
        } catch (RuntimeException | LinkageError e) {
            ClientEnvironment.logWarning("Key '" + setting.getDisplayName() + "' failed", e);
        }
    }
}
