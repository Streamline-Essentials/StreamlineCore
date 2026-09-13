package host.plas.configs.obj;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PermissionGroup {
    private String identifier;
    private String name;
    private String permission;

    public PermissionGroup(String identifier, String name, String permission) {
        setIdentifier(identifier);
        setName(name);
        setPermission(permission);
    }
}
