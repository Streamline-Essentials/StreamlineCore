package gg.drak.tacoessentials.commands;

/** Message styles, as {@code &} color codes, shared by every command. */
public final class Msg {

    private Msg() {}

    public static String success(String text) {
        return "&a" + text;
    }

    public static String info(String text) {
        return "&e" + text;
    }

    public static String error(String text) {
        return "&c" + text;
    }

    public static String muted(String text) {
        return "&7" + text;
    }

    /** A command's failure: its message is shown to the sender as an error and the command stops. */
    public static final class Fail extends Exception {
        public Fail(String message) {
            super(message, null, false, false);
        }
    }

    public static Fail fail(String message) {
        return new Fail(message);
    }
}
