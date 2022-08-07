package net.streamline.api.modules.dependencies;

import lombok.Getter;
import net.streamline.api.modules.dependencies.versioning.Versioning;

import java.util.List;

public class Dependency {
    @Getter
    private final String dependency;
    @Getter
    private final List<Versioning> majorVersion;
    @Getter
    private final List<Versioning> minorVersion;
    @Getter
    private final List<Versioning> buildVersion;

    public Dependency(String dependency, List<Versioning> majorVersion, List<Versioning> minorVersion, List<Versioning> buildVersion) {
        this.dependency = dependency;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.buildVersion = buildVersion;
    }
}
