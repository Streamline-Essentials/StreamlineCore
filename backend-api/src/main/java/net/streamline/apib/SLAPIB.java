package net.streamline.apib;

import lombok.Getter;
import lombok.Setter;

import java.io.File;

public class SLAPIB {
    @Getter @Setter
    private static SLAPIB instance;

    @Getter
    private final File dataFolder;

    public SLAPIB(File dataFolder) {
        instance = this;
        this.dataFolder = dataFolder;
    }
}
