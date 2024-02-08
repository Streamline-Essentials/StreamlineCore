package net.streamline.api.savables.datalizable.properties;

import lombok.Getter;

@Getter
public class Property {
    public static final Property UUID = new Property("uuid");

    public static final Property USERNAME = new Property("username");
    public static final Property DISPLAY_NAME = new Property("display_name");

    public static final Property SERVER = new Property("server");

    public static final Property WORLD = new Property("world");
    public static final Property POSITION = new Property("position");
    public static final Property ROTATION = new Property("rotation");

    public static final Property FIRST_JOIN = new Property("first_join");
    public static final Property LAST_JOIN = new Property("last_join");
    public static final Property LAST_QUIT = new Property("last_quit");


    private final String definedAs;

    public Property(String definedAs) {
        this.definedAs = definedAs;
    }

    public String get() {
        return definedAs;
    }
}
