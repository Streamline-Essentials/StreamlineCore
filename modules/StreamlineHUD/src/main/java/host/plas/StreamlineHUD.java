package host.plas;

import host.plas.hud.Formats;
import host.plas.hud.HudStyle;
import host.plas.hud.InfoElement;
import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.client.hud.HudRegistry;
import singularity.client.module.ClientModule;
import singularity.client.settings.ClientSettingsRegistry;
import singularity.client.settings.ModuleSettings;
import singularity.client.settings.Setting;

import java.util.Arrays;
import java.util.List;

/**
 * Client HUD elements built on Streamline's client API. Players move, scale and hide them
 * with {@code /slclient hud}, and style them on this module's page in {@code /slclient menu}.
 * Disabling or unloading it drops the elements and the settings page.
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

    public StreamlineHUD(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onEnable() {
        instance = this;

        // Registers whether or not the client handler is installed yet: the registries keep
        // elements and settings either way, and the client draws whatever is registered.
        for (InfoElement element : elements()) {
            HudStyle.track(element);
            HudRegistry.register(getIdentifier(), element);
        }

        ClientSettingsRegistry.register(settings());
    }

    @Override
    public void onDisable() {
        HudStyle.clear();
        // The module can be unloaded while the game runs; a static reference would keep its
        // class loader, and the jar copy it reads from, alive.
        instance = null;
    }

    private static List<InfoElement> elements() {
        return Arrays.asList(
                new InfoElement("fps", "FPS", LEFT, TOP, "&7FPS: &f144",
                        info -> "&7FPS: &f" + info.fps()),
                new InfoElement("coordinates", "Coordinates", LEFT, TOP + ROW, "&7XYZ: &f128.5 &7/ &f64.0 &7/ &f-42.5",
                        info -> {
                            int decimals = HudStyle.getCoordinateDecimals();
                            return "&7XYZ: &f" + Formats.number(info.x(), decimals)
                                    + " &7/ &f" + Formats.number(info.y(), decimals)
                                    + " &7/ &f" + Formats.number(info.z(), decimals);
                        }),
                new InfoElement("direction", "Direction", LEFT, TOP + ROW * 2, "&7Facing: &fNorth &7(-178.3)",
                        info -> "&7Facing: &f" + Formats.capitalize(info.facing())
                                + " &7(" + Formats.number(info.yaw(), 1) + ")"),
                new InfoElement("biome", "Biome", LEFT, TOP + ROW * 3, "&7Biome: &fPlains",
                        info -> info.biome().isEmpty() ? null : "&7Biome: &f" + Formats.idName(info.biome())),
                new InfoElement("dimension", "Dimension", LEFT, TOP + ROW * 4, "&7Dimension: &fOverworld",
                        info -> info.dimension().isEmpty() ? null : "&7Dimension: &f" + Formats.idName(info.dimension())),
                new InfoElement("ping", "Ping", RIGHT, TOP, "&7Ping: &f42ms",
                        info -> info.ping() < 0 ? null : "&7Ping: &f" + info.ping() + "ms"),
                new InfoElement("server", "Server", RIGHT, TOP + ROW, "&7Server: &fplay.example.net",
                        info -> info.serverAddress().isEmpty() ? null : "&7Server: &f" + info.serverAddress()),
                new InfoElement("clock", "Clock", RIGHT, TOP + ROW * 2, "&7Time: &f14:05",
                        info -> "&7Time: &f" + Formats.realClock(HudStyle.isTwelveHourClock())),
                new InfoElement("game-time", "In-game day and time", RIGHT, TOP + ROW * 3, "&7Day &f12 &7- &f18:30",
                        info -> "&7Day &f" + info.day() + " &7- &f"
                                + Formats.gameClock(info.dayTime(), HudStyle.isTwelveHourClock())),
                new InfoElement("memory", "Memory", RIGHT, TOP + ROW * 4, "&7Memory: &f1024&7/&f4096MB &7(25%)",
                        info -> Formats.memory())
        );
    }

    private ModuleSettings settings() {
        ModuleSettings settings = new ModuleSettings(getIdentifier(), "Streamline HUD",
                "Styles the HUD elements. Move, scale and hide them with /slclient hud "
                        + "(or Edit HUD layout below).");

        settings.add(new Setting.Color("text-color", "Text color", "Color of text without a color code.", 0xFFFFFFFF))
                .onChange(HudStyle::setTextColor);
        settings.add(new Setting.Toggle("background", "Background", "Draw a box behind each element.", true))
                .onChange(HudStyle::setBackground);
        settings.add(new Setting.Color("background-color", "Background color", "Color of the box behind each element.", 0x60000000))
                .onChange(HudStyle::setBackgroundColor);
        settings.add(new Setting.Toggle("shadow", "Text shadow", "Draw text with a drop shadow.", true))
                .onChange(HudStyle::setShadow);
        settings.add(new Setting.Slider("coordinate-decimals", "Coordinate decimals",
                        "Decimal places shown in coordinates.", 1, 0, 3, 1))
                .onChange(value -> HudStyle.setCoordinateDecimals((int) Math.round(value)));
        settings.add(new Setting.Choice("clock-format", "Clock format", "24-hour or 12-hour clocks.",
                        CLOCK_24H, Arrays.asList(CLOCK_24H, CLOCK_12H)))
                .onChange(value -> HudStyle.setTwelveHourClock(CLOCK_12H.equals(value)));

        return settings;
    }
}
