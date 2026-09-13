package host.plas.adapters.spigot;

import host.plas.StreamQuests;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SpigotAdapter {
    private SpigotListener listener;

    private boolean enabled;

    public SpigotAdapter() {
        if (tryEnable()) {
            StreamQuests.getInstance().logInfo("Successfully enabled: &cSpigotAdapter");
        } else {
            StreamQuests.getInstance().logInfo("Not running on Bukkit, disabling: &cSpigotAdapter");
        }
    }

    public static boolean isBukkit() {
        try {
            Class.forName("org.bukkit.Bukkit");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean tryEnable() {
        if (isBukkit()) {
            try {
                listener = new SpigotListener();

                setEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
                setEnabled(false);
            }
        } else {
            setEnabled(false);
        }

        return isEnabled();
    }
}
