package host.plas.config;

import gg.drak.thebase.objects.Identifiable;
import host.plas.data.DifferedSetup;
import host.plas.data.WebhookType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class WebhookSetup implements Identifiable {
    private WebhookType type;
    private String identifier;

    private String url;

    private DifferedSetup commands;
    private DifferedSetup chat;

    public WebhookSetup(WebhookType type, String identifier, String url, DifferedSetup commands, DifferedSetup chat) {
        this.type = type;
        this.identifier = identifier;
        this.url = url;
        this.commands = commands;
        this.chat = chat;
    }
}
