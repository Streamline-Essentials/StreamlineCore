package net.streamline.api.text;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

public class HexResulter implements Comparable<HexResulter> {
    @Getter @Setter
    private String starter;
    @Getter @Setter
    private String ender;

    public HexResulter(String starter, String ender) {
        this.starter = starter;
        this.ender = ender;
    }

    public HexResulter() {
        this("{#", "}");
    }

    public String getResult(String hex) {
        return starter + hex + ender;
    }

    public String getIdentifiably() {
        return getStarter() + "123456" + getEnder();
    }

    @Override
    public int compareTo(@NotNull HexResulter o) {
        return getIdentifiably().compareTo(o.getIdentifiably());
    }
}
