package host.plas.essentials.configured;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Setter
@Getter
public class ConfiguredIntegerPermission implements Comparable<ConfiguredIntegerPermission> {
    private int value;
    private String permission;

    public ConfiguredIntegerPermission(int value, String permission) {
        this.value = value;
        this.permission = permission;
    }

    @Override
    public int compareTo(@NotNull ConfiguredIntegerPermission o) {
        return Integer.compare(this.value, o.value);
    }
}
