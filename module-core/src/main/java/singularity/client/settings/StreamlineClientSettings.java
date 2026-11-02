package singularity.client.settings;

import singularity.client.ClientEnvironment;
import singularity.client.color.Chroma;
import singularity.client.hud.HudBoxStyle;

import java.io.File;
import java.util.Arrays;

/**
 * StreamlineClient's own settings: the Client Settings page at the top of the
 * {@code /slclient menu} sidebar. It belongs to no module, so it is never in
 * {@link ClientSettingsRegistry}, and its values are kept in {@code client/client-settings.properties}
 * where no module's settings file can collide with it.
 *
 * <p>Names and descriptions come from {@code client-common/client-settings.yml}
 * ({@code built-in.client-settings}), compiled into {@link BuiltInSettingTexts}.</p>
 *
 * <p>The HUD settings apply to every module's elements: backgrounds and text shadow only draw
 * while both this page and the element allow them, and Strip Colors / Chroma Everywhere rewrite
 * every HUD line as it is drawn.</p>
 */
public final class StreamlineClientSettings extends ModuleSettings {

    /** This page's id in {@code client-settings.yml}. */
    public static final String TYPE = "client-settings";

    private static final StreamlineClientSettings PAGE = new StreamlineClientSettings();

    public static final Setting.Toggle SHOW_HUD = PAGE.add(new Setting.Toggle("show-hud",
            name("show-hud"), description("show-hud"), true));
    public static final Setting.Toggle BACKGROUND = PAGE.add(new Setting.Toggle("background",
            name("background"), description("background"), true));
    public static final Setting.Color BACKGROUND_COLOR = PAGE.add(new Setting.Color("background-color",
            name("background-color"), description("background-color"), HudBoxStyle.DEFAULT_BACKGROUND_COLOR));
    public static final Setting.Toggle TEXT_SHADOW = PAGE.add(new Setting.Toggle("text-shadow",
            name("text-shadow"), description("text-shadow"), true));
    public static final Setting.Toggle STRIP_COLORS = PAGE.add(new Setting.Toggle("strip-colors",
            name("strip-colors"), description("strip-colors"), false));
    public static final Setting.Toggle CHROMA_EVERYWHERE = PAGE.add(new Setting.Toggle("chroma-everywhere",
            name("chroma-everywhere"), description("chroma-everywhere"), false));
    public static final Setting.Toggle REMEMBER_PAGE = PAGE.add(new Setting.Toggle("remember-page",
            name("remember-page"), description("remember-page"), true));
    public static final Setting.Toggle CHECK_ECLOUD = PAGE.add(new Setting.Toggle("check-ecloud",
            name("check-ecloud"), description("check-ecloud"), true));

    public static final Setting.Choice CHROMA_TYPE = PAGE.add(new Setting.Choice("chroma-type",
            name("chroma-type"), description("chroma-type"), "Static", Arrays.asList("Static", "Breathing", "Pulsing")));
    public static final Setting.Slider CHROMA_CYCLE = PAGE.add(new Setting.Slider("chroma-cycle",
            name("chroma-cycle"), description("chroma-cycle"), 5.0, 0.5, 30.0, 0.5));
    public static final Setting.Slider CHROMA_SATURATION = PAGE.add(new Setting.Slider("chroma-saturation",
            name("chroma-saturation"), description("chroma-saturation"), 100.0, 0.0, 100.0, 1.0));
    public static final Setting.Slider CHROMA_BRIGHTNESS = PAGE.add(new Setting.Slider("chroma-brightness",
            name("chroma-brightness"), description("chroma-brightness"), 100.0, 0.0, 100.0, 1.0));

    static {
        CHROMA_TYPE.onChange(value -> Chroma.setDefaultType(Chroma.Type.byId(value)));
        CHROMA_CYCLE.onChange(Chroma::setCycleSeconds);
        CHROMA_SATURATION.onChange(value -> Chroma.setSaturation(value / 100.0));
        CHROMA_BRIGHTNESS.onChange(value -> Chroma.setBrightness(value / 100.0));
    }

    private StreamlineClientSettings() {
        super("streamlineclient", "Client Settings", "Settings for StreamlineClient itself.");
    }

    private static String name(String key) {
        return BuiltInSettingTexts.name(TYPE, key);
    }

    private static String description(String key) {
        return BuiltInSettingTexts.description(TYPE, key);
    }

    /** @return the Client Settings page */
    public static StreamlineClientSettings page() {
        return PAGE;
    }

    /** Reads the saved values; called once the client folder is set. */
    public static void loadSaved() {
        PAGE.load();
    }

    @Override
    protected File file() {
        return ClientEnvironment.file("client-settings.properties");
    }

    public static boolean showHud() {
        return Boolean.TRUE.equals(SHOW_HUD.getValue());
    }

    /** @return whether HUD elements may draw their own backgrounds at all */
    public static boolean background() {
        return Boolean.TRUE.equals(BACKGROUND.getValue());
    }

    /** @return the ARGB background for HUD elements that keep the default color */
    public static int backgroundColor() {
        Integer color = BACKGROUND_COLOR.getValue();
        return color == null ? HudBoxStyle.DEFAULT_BACKGROUND_COLOR : color;
    }

    /** @return whether HUD text may be drawn with a drop shadow at all */
    public static boolean textShadow() {
        return Boolean.TRUE.equals(TEXT_SHADOW.getValue());
    }

    /** @return whether HUD text is drawn without its colors */
    public static boolean stripColors() {
        return Boolean.TRUE.equals(STRIP_COLORS.getValue());
    }

    /** @return whether HUD text is drawn in the chroma color, whatever its own colors */
    public static boolean chromaEverywhere() {
        return Boolean.TRUE.equals(CHROMA_EVERYWHERE.getValue());
    }

    public static boolean rememberPage() {
        return Boolean.TRUE.equals(REMEMBER_PAGE.getValue());
    }

    public static boolean checkECloud() {
        return Boolean.TRUE.equals(CHECK_ECLOUD.getValue());
    }
}
