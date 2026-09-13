package host.plas.configs.bits;

public enum CheckType {
    EQUALS,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    REGEX,
    NOT_EQUALS,
    NOT_CONTAINS,
    NOT_STARTS_WITH,
    NOT_ENDS_WITH,
    ;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
