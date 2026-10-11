package singularity.client.settings;

import singularity.client.ClientEnvironment;
import singularity.client.color.Chroma;

import java.io.File;
import java.util.Arrays;

/**
 * StreamlineClient's own settings: the Client Settings page at the top of the
 * {@code /slclient menu} sidebar. It belongs to no module, so it is never in
 * {@link ClientSettingsRegistry}, and its values are kept in {@code client/client-settings.properties}
 * where no module's settings file can collide with it.
 */
public final class StreamlineClientSettings extends ModuleSettings {

    private static final StreamlineClientSettings PAGE = new StreamlineClientSettings();

    public static final Setting.Toggle SHOW_HUD = PAGE.add(new Setting.Toggle("show-hud", "Show HUD",
            "Draws module HUD elements in game", true));
    public static final Setting.Toggle REMEMBER_PAGE = PAGE.add(new Setting.Toggle("remember-page", "Remember Last Page",
            "Reopens this menu on the page it was closed on", true));
    public static final Setting.Toggle CHECK_ECLOUD = PAGE.add(new Setting.Toggle("check-ecloud", "Check eCloud For Updates",
            "Asks eCloud for newer versions when the menu opens", true));

    public static final Setting.Choice CHROMA_TYPE = PAGE.add(new Setting.Choice("chroma-type", "Chroma Type",
            "What %streamlineclient_colors_chroma% uses", "Static", Arrays.asList("Static", "Breathing", "Pulsing")));
    public static final Setting.Slider CHROMA_CYCLE = PAGE.add(new Setting.Slider("chroma-cycle", "Chroma Cycle Seconds",
            "Time for the chroma hue to go once round", 5.0, 0.5, 30.0, 0.5));
    public static final Setting.Slider CHROMA_SATURATION = PAGE.add(new Setting.Slider("chroma-saturation", "Chroma Saturation",
            "Percent; lower is more pastel", 100.0, 0.0, 100.0, 1.0));
    public static final Setting.Slider CHROMA_BRIGHTNESS = PAGE.add(new Setting.Slider("chroma-brightness", "Chroma Brightness",
            "Percent; the peak for breathing and pulsing", 100.0, 0.0, 100.0, 1.0));

    static {
        CHROMA_TYPE.onChange(value -> Chroma.setDefaultType(Chroma.Type.byId(value)));
        CHROMA_CYCLE.onChange(Chroma::setCycleSeconds);
        CHROMA_SATURATION.onChange(value -> Chroma.setSaturation(value / 100.0));
        CHROMA_BRIGHTNESS.onChange(value -> Chroma.setBrightness(value / 100.0));
    }

    private StreamlineClientSettings() {
        super("streamlineclient", "Client Settings", "Settings for StreamlineClient itself.");
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

    public static boolean rememberPage() {
        return Boolean.TRUE.equals(REMEMBER_PAGE.getValue());
    }

    public static boolean checkECloud() {
        return Boolean.TRUE.equals(CHECK_ECLOUD.getValue());
    }
}
