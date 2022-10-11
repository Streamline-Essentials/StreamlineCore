package net.streamline.api.interfaces;

import com.mongodb.lang.Nullable;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public interface IMessenger {
    void sendMessage(@Nullable StreamlineUser to, String message);

    void sendMessage(@Nullable StreamlineUser to, String otherUUID, String message);

    void sendMessage(@Nullable StreamlineUser to, StreamlineUser other, String message);

    void sendTitle(StreamlinePlayer user, StreamlineTitle title);

    String codedString(String from);

    String stripColor(String string);
}
