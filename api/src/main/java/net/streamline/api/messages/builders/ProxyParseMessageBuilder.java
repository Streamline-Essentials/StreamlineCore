package net.streamline.api.messages.builders;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.ProxyMessageHelper;
import net.streamline.api.messages.ProxyMessageIn;
import net.streamline.api.messages.ProxyMessageOut;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.ArrayList;
import java.util.List;

public class ProxyParseMessageBuilder {
    @Getter
    private static final String subChannel = "api-parse-send";

    @Getter
    private static final List<String> lines = List.of(
            "parse=%this_parse%;",
            "user_uuid=%this_user_uuid%;"
    );

    public static ProxyMessageOut build(String toParse, StreamlineUser user) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        output.writeUTF(getSubChannel());
        output.writeUTF(lines.get(0).replace("%this_parse%", toParse));
        output.writeUTF(lines.get(1).replace("%this_user_uuid%", user.getUUID()));

        return new ProxyMessageOut(SLAPI.getApiChannel(), getSubChannel(), output.toByteArray());
    }

    public static SingleSet<String, String> unbuild(ProxyMessageIn messageIn) {
        List<StreamlineUser> users = new ArrayList<>();
        ByteArrayDataInput input = ByteStreams.newDataInput(messageIn.getMessages());
        if (! messageIn.getSubChannel().equals(input.readUTF())) {
            SLAPI.getInstance().getMessenger().logWarning("Data mis-match on ProxyMessageIn for '" + ProxyParseMessageBuilder.class.getSimpleName() + "'. Continuing anyway...");
        }

        List<String> l = new ArrayList<>();
        l.add(input.readUTF());
        l.add(input.readUTF());

        String toParse = ProxyMessageHelper.extrapolate(l.get(0)).value;
        String uuid = ProxyMessageHelper.extrapolate(l.get(1)).value;

        return new SingleSet<>(toParse, uuid);
    }
}
