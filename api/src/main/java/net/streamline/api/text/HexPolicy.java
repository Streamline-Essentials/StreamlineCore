package net.streamline.api.text;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
public class HexPolicy implements Comparable<HexPolicy> {
    @Setter
    private String starter;
    @Setter
    private String ender;

    public HexPolicy(String starter, String ender) {
        this.starter = starter;
        this.ender = ender;
    }

    public HexPolicy() {
        this("{#", "}");
    }

    public String getResult(String hex) {
        return starter + hex + ender;
    }

    public String getIdentifiably() {
        return getStarter() + "123456" + getEnder();
    }

    @Override
    public int compareTo(@NotNull HexPolicy o) {
        return getIdentifiably().compareTo(o.getIdentifiably());
    }
}
