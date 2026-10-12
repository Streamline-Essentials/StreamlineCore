package host.plas;

import host.plas.hud.CustomElements;
import host.plas.hud.ElementStore;
import host.plas.hud.HudStyle;
import host.plas.hud.Placeholders;
import host.plas.hud.TemplateElement;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.client.ClientInfo;
import singularity.client.hud.HudRegistry;
import singularity.client.module.ClientModule;
import singularity.client.settings.ClientSettingsRegistry;
import singularity.client.settings.ModuleSettings;
import singularity.client.settings.Setting;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Client HUD elements built on Streamline's client API. Players move, scale and hide them
 * with {@code /slclient hud}, and on this module's page in {@code /slclient menu} they style
 * them, edit each element's text (with {@code %streamlinehud_...%} placeholders), and add text
 * elements of their own. Disabling or unloading it drops the elements and the settings page.
 */
public class StreamlineHUD extends ClientModule {
    @Getter @Setter
    private static StreamlineHUD instance;

    private static final String CLOCK_24H = "24h";
    private static final String CLOCK_12H = "12h";

    /** Left column top to bottom, then right column; positions are fractions of the screen. */
    private static final double LEFT = 0.005;
    private static final double RIGHT = 0.86;
    private static final double ROW = 0.045;
    private static final double TOP = 0.01;

    private static final String MAIN = Placeholders.token("color_main");
    private static final String TEXT = Placeholders.token("color_text");
    private static final Pattern CUSTOM_ID = Pattern.compile("[a-z0-9_-]{1,32}");

    /** Every element this module registered, built-in and added, by id. */
    private final Map<String, TemplateElement> elements = new ConcurrentHashMap<>();
    private ElementStore store;

    public StreamlineHUD(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onEnable() {
        instance = this;
        store = new ElementStore(new File(getDataFolder(), "elements.properties"), this::logWarning);
        store.load();

        // Registers whether or not the client handler is installed yet: the registries keep
        // elements and settings either way, and the client draws whatever is registered.
        for (TemplateElement element : builtIns()) {
            element.load(store.template(element.getId()));
            add(element);
        }
        for (Map.Entry<String, String> entry : store.custom().entrySet()) {
            TemplateElement element = custom(entry.getKey(), entry.getValue());
            add(element);
        }

        ClientSettingsRegistry.register(settings());

        try {
            CustomElements.install(this);
        } catch (LinkageError e) {
            logInfo("This StreamlineCore cannot add HUD text from the menu; update the mod to add your own elements.");
        }
    }

    @Override
    public void onDisable() {
        HudStyle.clear();
        elements.clear();
        store = null;
        // The module can be unloaded while the game runs; a static reference would keep its
        // class loader, and the jar copy it reads from, alive.
        instance = null;
    }

    private void add(TemplateElement element) {
        elements.put(element.getId(), element);
        HudStyle.track(element);
        HudRegistry.register(getIdentifier(), element);
    }

    private void saved(TemplateElement element) {
        ElementStore current = store;
        if (current != null) current.saved(element);
    }

    private TemplateElement element(String id, String name, double x, double y, String template, Predicate<ClientInfo> shown) {
        return new TemplateElement(id, name, x, y, template, shown, false, this::saved);
    }

    /** An element the player added: it starts at the top-left corner and shows its id in the menu. */
    private TemplateElement custom(String id, String template) {
        return new TemplateElement(id, id, 0, 0, template, null, true, this::saved);
    }

    /**
     * Adds a text element from the module menu.
     *
     * @return {@code null} once added, otherwise why not
     */
    public String addCustom(String id, String template) {
        String key = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        if (! CUSTOM_ID.matcher(key).matches()) return "Use 1 to 32 lowercase letters, digits, - or _ for the identifier.";
        if (elements.containsKey(key)) return "There is already an element called '" + key + "'.";
        if (template == null || template.trim().isEmpty()) return "Type the text to show.";
        TemplateElement element = custom(key, template);
        HudRegistry.forgetPlacement(getIdentifier(), key);
        add(element);
        saved(element);
        HudRegistry.save();
        return null;
    }

    /**
     * Deletes a text element the player added.
     *
     * @return {@code null} once removed, otherwise why not
     */
    public String removeCustom(String id) {
        TemplateElement element = elements.get(id);
        if (element == null) return "There is no element called '" + id + "'.";
        if (! element.isCustom()) return "Only elements you added can be deleted; hide this one instead.";
        elements.remove(id);
        HudRegistry.unregister(getIdentifier(), id);
        HudStyle.untrack(element);
        ElementStore current = store;
        if (current != null) current.removeCustom(id);
        return null;
    }

    private List<TemplateElement> builtIns() {
        return Arrays.asList(
                element("fps", "FPS", LEFT, TOP,
                        MAIN + "FPS: " + TEXT + token("fps"), null),
                element("coordinates", "Coordinates", LEFT, TOP + ROW,
                        MAIN + "XYZ: " + TEXT + token("x") + " " + MAIN + "/ " + TEXT + token("y")
                                + " " + MAIN + "/ " + TEXT + token("z"), null),
                element("direction", "Direction", LEFT, TOP + ROW * 2,
                        MAIN + "Facing: " + TEXT + token("facing") + " " + MAIN + "(" + token("yaw") + ")", null),
                element("biome", "Biome", LEFT, TOP + ROW * 3,
                        MAIN + "Biome: " + TEXT + token("biome"), info -> ! info.biome().isEmpty()),
                element("dimension", "Dimension", LEFT, TOP + ROW * 4,
                        MAIN + "Dimension: " + TEXT + token("dimension"), info -> ! info.dimension().isEmpty()),
                element("ping", "Ping", RIGHT, TOP,
                        MAIN + "Ping: " + TEXT + token("ping") + "ms", info -> info.ping() >= 0),
                element("server", "Server", RIGHT, TOP + ROW,
                        MAIN + "Server: " + TEXT + token("server"), info -> ! info.serverAddress().isEmpty()),
                element("clock", "Clock", RIGHT, TOP + ROW * 2,
                        MAIN + "Time: " + TEXT + token("clock"), null),
                element("game-time", "In-Game Day And Time", RIGHT, TOP + ROW * 3,
                        MAIN + "Day " + TEXT + token("day") + " " + MAIN + "- " + TEXT + token("game_time"), null),
                element("memory", "Memory", RIGHT, TOP + ROW * 4,
                        MAIN + "Memory: " + TEXT + token("memory_used") + MAIN + "/" + TEXT + token("memory_max")
                                + "MB " + MAIN + "(" + token("memory_percent") + "%)", null)
        );
    }

    private static String token(String key) {
        return Placeholders.token(key);
    }

    private ModuleSettings settings() {
        ModuleSettings settings = new ModuleSettings(getIdentifier(), "Streamline HUD",
                "Styles the HUD elements. Edit an element's text with its Edit button below, add your own with +, "
                        + "and move, scale and hide them with /slclient hud (or Edit HUD Layout below).");

        settings.add(new Setting.Color("main-color", "Main Color",
                        "Used for labels. Has a placeholder.", HudStyle.DEFAULT_MAIN_COLOR))
                .onChange(HudStyle::setMainColor);
        settings.add(new Setting.Color("text-color", "Text Color",
                        "Used for uncolored text. Has a placeholder.", HudStyle.DEFAULT_TEXT_COLOR))
                .onChange(HudStyle::setTextColor);
        settings.add(new Setting.Toggle("background", "Background", "Draw a box behind each element.", true))
                .onChange(HudStyle::setBackground);
        settings.add(new Setting.Color("background-color", "Background Color", "Color of the box behind each element.", 0x60000000))
                .onChange(HudStyle::setBackgroundColor);
        settings.add(new Setting.Toggle("shadow", "Text Shadow", "Draw text with a drop shadow.", true))
                .onChange(HudStyle::setShadow);
        settings.add(new Setting.Slider("coordinate-decimals", "Coordinate Decimals",
                        "Decimal places shown in coordinates.", 1, 0, 3, 1))
                .onChange(value -> HudStyle.setCoordinateDecimals((int) Math.round(value)));
        settings.add(new Setting.Choice("clock-format", "Clock Format", "24-hour or 12-hour clocks.",
                        CLOCK_24H, Arrays.asList(CLOCK_24H, CLOCK_12H)))
                .onChange(value -> HudStyle.setTwelveHourClock(CLOCK_12H.equals(value)));

        return settings;
    }
}
