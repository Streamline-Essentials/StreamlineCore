package net.streamline.api.data.players.permissions;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import org.jetbrains.annotations.NotNull;

@Getter @Setter
public class SenderPermissions implements Comparable<SenderPermissions> {
    private StreamSender sender;

    private boolean bypassingPermissions;

    public SenderPermissions(StreamSender sender) {
        this.sender = sender;
        this.bypassingPermissions = false;
    }

    @Override
    public int compareTo(@NotNull SenderPermissions o) {
        return sender.compareTo(o.getSender());
    }

    public boolean hasPermission(String permission) {
        if (isBypassingPermissions()) return true;

        return sender.asReal().hasPermission(permission);
    }

    public void addPermission(String permission) {
        sender.asReal().addPermission(permission);
    }

    public void removePermission(String permission) {
        sender.asReal().removePermission(permission);
    }
}
