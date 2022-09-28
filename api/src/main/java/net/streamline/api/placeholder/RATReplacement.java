package net.streamline.api.placeholder;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

public class RATReplacement implements Comparable<RATReplacement> {
    @Getter @Setter
    private String total;
    @Getter @Setter
    private String identifier;
    @Getter @Setter
    private String params;
    @Getter @Setter
    private String replacement;

    public RATReplacement(String total, String identifier, String params, String replacement) {
        setTotal(total);
        setIdentifier(identifier);
        setParams(params);
        setReplacement(replacement);
    }

    @Override
    public int compareTo(@NotNull RATReplacement o) {
        return CharSequence.compare(getTotal(), o.getTotal());
    }

    @Override
    public String toString() {
        return "RATReplacement[ " + getTotal() + ":" + getReplacement() + " ]";
    }
}
