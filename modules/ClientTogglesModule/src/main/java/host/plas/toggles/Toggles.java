package host.plas.toggles;

import singularity.client.hud.HudRegistry;
import singularity.client.input.ClientInput;
import singularity.client.settings.ClientSettingsRegistry;
import singularity.client.settings.ModuleSettings;
import singularity.client.settings.Setting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * The toggles themselves, their settings page and HUD elements. Kept apart from
 * {@link ClientTogglesModule} because it links API that older StreamlineCore builds lack.
 *
 * <p>The toggled state lives here, not on the player, so dying, respawning or changing worlds
 * leaves it as it was; {@link ClientInput} presses the held keys again every tick.</p>
 */
final class Toggles {

    /** Below StreamlineHUD's left column, clear of the chat in the bottom-left corner. */
    private static final double LEFT = 0.005;
    private static final double TOP = 0.235;
    private static final double ROW = 0.045;

    private static volatile String moduleId;
    private static volatile boolean sprinting;
    private static volatile boolean sneaking;

    private static volatile String yes = "Yes";
    private static volatile String no = "No";
    private static volatile String sprintingOn = "Sprinting Toggled";
    private static volatile String sprintingOff = "Sprinting Not Toggled";
    private static volatile String sneakingOn = "Sneaking Toggled";
    private static volatile String sneakingOff = "Sneaking Not Toggled";

    private static volatile File elementsFile;
    private static volatile ClientTogglesModule owner;

    private Toggles() {}

    static void install(ClientTogglesModule module) {
        owner = module;
        moduleId = module.getIdentifier();
        elementsFile = new File(module.getDataFolder(), "elements.properties");
        Properties saved = loadTemplates();

        ToggleElement sprint = new ToggleElement("sprinting", "Sprint Toggle", LEFT, TOP,
                "&7" + Placeholders.SPRINTING_TEXT, Toggles::saveTemplate);
        ToggleElement sneak = new ToggleElement("sneaking", "Sneak Toggle", LEFT, TOP + ROW,
                "&7" + Placeholders.SNEAKING_TEXT, Toggles::saveTemplate);
        sprint.load(saved.getProperty(sprint.getId()));
        sneak.load(saved.getProperty(sneak.getId()));
        HudRegistry.register(moduleId, sprint);
        HudRegistry.register(moduleId, sneak);

        ClientSettingsRegistry.register(settings(module.getIdentifier()));
    }

    static void uninstall(ClientTogglesModule module) {
        String id = module.getIdentifier();
        setSprinting(false);
        setSneaking(false);
        ClientInput.forgetModule(id);
        moduleId = null;
        elementsFile = null;
        owner = null;
    }

    private static ModuleSettings settings(String id) {
        ModuleSettings settings = new ModuleSettings(id, "Client Toggles",
                "Toggle sprint and sneak with a key press; they stay on through deaths and respawns. "
                        + "Click a key to bind it, press Escape to cancel, or right-click to unbind.");

        settings.add(new Setting.Key("sprint-key", "Toggle Sprint Key", "Turns toggle sprint on or off.", "key.keyboard.r"))
                .onPress(() -> setSprinting(! sprinting));
        settings.add(new Setting.Key("sneak-key", "Toggle Sneak Key", "Turns toggle sneak on or off.", "key.keyboard.g"))
                .onPress(() -> setSneaking(! sneaking));

        settings.add(new Setting.Text("yes-text", "Toggled Value",
                        "What the _toggled placeholders show while toggled.", "Yes", 64))
                .onChange(value -> yes = value);
        settings.add(new Setting.Text("no-text", "Not Toggled Value",
                        "What the _toggled placeholders show while not toggled.", "No", 64))
                .onChange(value -> no = value);
        settings.add(new Setting.Text("sprinting-on-text", "Sprinting Toggled Text",
                        "What %clienttogglesmodule_sprinting_text% shows while toggled.", "Sprinting Toggled", 64))
                .onChange(value -> sprintingOn = value);
        settings.add(new Setting.Text("sprinting-off-text", "Sprinting Not Toggled Text",
                        "What %clienttogglesmodule_sprinting_text% shows while not toggled.", "Sprinting Not Toggled", 64))
                .onChange(value -> sprintingOff = value);
        settings.add(new Setting.Text("sneaking-on-text", "Sneaking Toggled Text",
                        "What %clienttogglesmodule_sneaking_text% shows while toggled.", "Sneaking Toggled", 64))
                .onChange(value -> sneakingOn = value);
        settings.add(new Setting.Text("sneaking-off-text", "Sneaking Not Toggled Text",
                        "What %clienttogglesmodule_sneaking_text% shows while not toggled.", "Sneaking Not Toggled", 64))
                .onChange(value -> sneakingOff = value);
        return settings;
    }

    static void setSprinting(boolean on) {
        sprinting = on;
        String id = moduleId;
        if (id != null) ClientInput.hold(id, ClientInput.GameKey.SPRINT, on);
    }

    static void setSneaking(boolean on) {
        sneaking = on;
        String id = moduleId;
        if (id != null) ClientInput.hold(id, ClientInput.GameKey.SNEAK, on);
    }

    static boolean isSprinting() {
        return sprinting;
    }

    static boolean isSneaking() {
        return sneaking;
    }

    static String yesNo(boolean toggled) {
        return toggled ? yes : no;
    }

    static String sprintingText() {
        return sprinting ? sprintingOn : sprintingOff;
    }

    static String sneakingText() {
        return sneaking ? sneakingOn : sneakingOff;
    }

    // ---------------------------------------------------------------- element text

    private static Properties loadTemplates() {
        Properties properties = new Properties();
        File file = elementsFile;
        if (file == null || ! file.isFile()) return properties;
        try (InputStream in = new FileInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            warn("Could not read " + file + ": " + e.getMessage());
        }
        return properties;
    }

    /** Keeps an element's text; an element back at its default text is left out. */
    private static synchronized void saveTemplate(ToggleElement element) {
        File file = elementsFile;
        if (file == null) return;
        Properties properties = loadTemplates();
        if (element.isEdited()) properties.setProperty(element.getId(), element.getTemplate());
        else properties.remove(element.getId());
        file.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(file)) {
            properties.store(out, "Client Toggles HUD element text");
        } catch (IOException e) {
            warn("Could not write " + file + ": " + e.getMessage());
        }
    }

    private static void warn(String message) {
        ClientTogglesModule module = owner;
        if (module != null) module.logWarning(message);
    }
}
