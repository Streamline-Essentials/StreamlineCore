package net.streamline.api.placeholder;

public class PlaceholderValue {
    public boolean isEmpty;
    public String unparsed;
    public String identifier;
    public String params;
    public String parsed;

    public PlaceholderValue(String unparsed, String identifier, String params) {
        this.unparsed = unparsed;
        this.identifier = identifier;
        this.params = params;
        this.parsed = "";
        updateEmptiness();
    }

    public PlaceholderValue() {
        this.unparsed = "";
        this.identifier = "";
        this.params = "";
        this.parsed = "";
        updateEmptiness();
    }

    public PlaceholderValue updateEmptiness() {
        this.isEmpty = this.unparsed.equals("") && this.identifier.equals("") && this.params.equals("");
        return this;
    }

    public PlaceholderValue setUnparsed(String unparsed) {
        this.unparsed = unparsed;
        return this;
    }

    public PlaceholderValue setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public PlaceholderValue setParams(String params) {
        this.params = params;
        return this;
    }

    public PlaceholderValue setParsed(String parsed) {
        this.parsed = parsed;
        return this;
    }

    @Override
    public String toString() {
        return "PlaceholderValue{ Is Empty: " + this.isEmpty + " , Unparsed:" + this.unparsed + " ; [ " + this.identifier + " , " + this.params + " ] }";
    }
}
