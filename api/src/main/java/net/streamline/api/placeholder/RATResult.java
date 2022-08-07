package net.streamline.api.placeholder;

public class RATResult {
    public String string;
    public int thingsReplaced;

    public RATResult(String string, int thingsReplaced) {
        this.string = string;
        this.thingsReplaced = thingsReplaced;
    }

    public boolean didReplacement() {
        return thingsReplaced > 0;
    }
}
