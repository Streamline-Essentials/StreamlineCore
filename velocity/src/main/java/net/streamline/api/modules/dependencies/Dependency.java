package net.streamline.api.modules.dependencies;

import lombok.Getter;
import net.streamline.api.modules.dependencies.versioning.Versioning;

import java.util.List;

public record Dependency(@Getter String dependency, @Getter List<Versioning> majorVersion,
                         @Getter List<Versioning> minorVersion, @Getter List<Versioning> buildVersion) {
}
