package net.streamline.api.interfaces;

public interface IStreamline {
    enum PlatformType {
        BUNGEE,
        SPIGOT,
        VELOCITY,
        ;
    }
    enum ServerType {
        PROXY,
        BACKEND,
        STANDALONE,
        ;
    }

    PlatformType getPlatformType();

    ServerType getServerType();
}
