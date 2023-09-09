package net.streamline.api.board.data;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ConcurrentHashMap;

public class BoardHeader {
    public static final String EMPTY = ">>EMPTY<<";

    @Getter @Setter
    private String header;

    public BoardHeader(String header) {
        this.header = header;
    }

    public BoardHeader(Class<?> clazz) {
        this.header = clazz.getSimpleName();
        if (! this.header.endsWith(".class"))
            this.header = this.header.concat(".class");
    }

    public BoardHeader() {
        this.header = EMPTY;
    }

    public boolean isEmpty() {
        return this.header.equals(EMPTY);
    }

    public boolean isOfAnyClass() {
        return this.header.endsWith(".class");
    }

    public boolean isOfClass(Class<?> clazz) {
        return this.header.equals(clazz.getSimpleName());
    }
}