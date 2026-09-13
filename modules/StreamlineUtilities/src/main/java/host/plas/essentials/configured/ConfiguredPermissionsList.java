package host.plas.essentials.configured;

import gg.drak.thebase.lib.leonhard.storage.sections.FlatFileSection;
import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Setter
@Getter
public class ConfiguredPermissionsList {
    public static final int DEFAULT_VALUE = 0;
    public static final String BASE_PERMISSION = "--";

    private int defaultValue;
    private String basePermission;
    private ConcurrentSkipListSet<ConfiguredIntegerPermission> permissions;

    public ConfiguredPermissionsList(ConcurrentSkipListSet<ConfiguredIntegerPermission> permissions, int defaultValue, String basePermission) {
        this.permissions = permissions;
        this.defaultValue = defaultValue;
        this.basePermission = basePermission;
    }

    public ConfiguredPermissionsList(ConcurrentSkipListSet<ConfiguredIntegerPermission> permissions) {
        this(permissions, DEFAULT_VALUE, BASE_PERMISSION);
    }

    public ConfiguredPermissionsList(int defaultValue, String basePermission) {
        this(new ConcurrentSkipListSet<>(), defaultValue, basePermission);
    }

    public ConfiguredPermissionsList(int defaultValue) {
        this(new ConcurrentSkipListSet<>(), defaultValue, BASE_PERMISSION);
    }

    public ConfiguredPermissionsList(String basePermission) {
        this(new ConcurrentSkipListSet<>(), DEFAULT_VALUE, basePermission);
    }

    public ConfiguredPermissionsList(int defaultValue, ConcurrentSkipListSet<ConfiguredIntegerPermission> permissions) {
        this(permissions, defaultValue, BASE_PERMISSION);
    }

    public ConfiguredPermissionsList(String basePermission, ConcurrentSkipListSet<ConfiguredIntegerPermission> permissions) {
        this(permissions, DEFAULT_VALUE, basePermission);
    }

    public ConfiguredPermissionsList() {
        this(new ConcurrentSkipListSet<>());
    }

    public ConfiguredPermissionsList(FlatFileSection section) {
        this(map(section));
    }

    public void addPermission(int value, String permission) {
        addPermission(new ConfiguredIntegerPermission(value, permission));
    }

    public void addPermission(ConfiguredIntegerPermission permission) {
        this.permissions.add(permission);
    }

    public void removePermission(int value) {
        this.permissions.forEach(permission -> {
            if (permission.getValue() == value) {
                this.permissions.remove(permission);
            }
        });
    }

    public ConfiguredIntegerPermission getPermission(int value) {
        AtomicReference<ConfiguredIntegerPermission> permission = new AtomicReference<>(null);

        this.permissions.forEach(p -> {
            if (permission.get() != null) return;
            if (p.getValue() == value) {
                permission.set(p);
            }
        });

        if (permission.get() == null) {
            AtomicReference<ConfiguredIntegerPermission> checking = new AtomicReference<>(null);
            this.permissions.forEach(p -> {
                if (p.getValue() <= value) {
                    if (checking.get() == null) {
                        checking.set(p);
                    } else {
                        if (p.getValue() > checking.get().getValue()) {
                            checking.set(p);
                        }
                    }
                }
            });

            permission.set(checking.get());
        }

        ConfiguredIntegerPermission p = permission.get();
        if (p == null) {
            p = new ConfiguredIntegerPermission(DEFAULT_VALUE, "streamline.utils.default");
        }

        return p;
    }

    public String getBasePermissionCasually() {
        String basePermission;

        if (getBasePermission().equals("--")) {
            basePermission = "";
        } else {
            basePermission = getBasePermission();
        }

        return basePermission;
    }

    public int getTopValue(CosmicSender user) {
        AtomicInteger top = new AtomicInteger(getDefaultValue());

        getPermissions().forEach(permission -> {
            if (user.hasPermission(getBasePermissionCasually() + permission.getPermission())) {
                if (permission.getValue() > top.get()) {
                    top.set(permission.getValue());
                }
            }
        });

        return top.get();
    }

    public String getTopPermission(CosmicSender user) {
        return getBasePermissionCasually() + getPermission(getTopValue(user)).getPermission();
    }

    public int getBottomValue(CosmicSender user) {
        AtomicInteger bottom = new AtomicInteger(getDefaultValue());

        getPermissions().forEach(permission -> {
            if (ModuleUtils.hasPermission(user, getBasePermissionCasually() + permission.getPermission())) {
                if (permission.getValue() < bottom.get()) {
                    bottom.set(permission.getValue());
                }
            }
        });

        return bottom.get();
    }

    public String getBottomPermission(CosmicSender user) {
        return getBasePermissionCasually() + getPermission(getBottomValue(user)).getPermission();
    }

    public boolean hasPermission(CosmicSender user, int value) {
        return user.hasPermission(getBasePermissionCasually() + getPermission(value).getPermission());
    }

    public static ConcurrentSkipListSet<ConfiguredIntegerPermission> map(FlatFileSection section) {
        ConcurrentSkipListSet<ConfiguredIntegerPermission> permissions = new ConcurrentSkipListSet<>();

        section.singleLayerKeySet().forEach(key -> {
            try {
                permissions.add(new ConfiguredIntegerPermission(Integer.parseInt(key), section.getString(key)));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });

        return permissions;
    }
}
