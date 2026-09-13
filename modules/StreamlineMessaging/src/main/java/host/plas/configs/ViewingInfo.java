package host.plas.configs;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ViewingInfo {
    private String permission;
    private String togglePermission;

    public ViewingInfo(String permission, String togglePermission) {
        this.permission = permission;
        this.togglePermission = togglePermission;
    }
}
