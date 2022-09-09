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
import net.streamline.api.savables.users.StreamlineUser;

import java.util.ArrayList;
import java.util.List;

public class ReturnParseMessageBuilder {
    @Getter
    private static final String subChannel = "api-parse-return";

    @Getter
    private static final List<String> lines = List.of(
            "parse=%this_parse%;",
            "parsed=%this_parsed%;",
            "user_uuid=%this_user_uuid%;"
    );

    public static ProxyMessageOut build(String gotParsed, String parsedAs, StreamlineUser user) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        output.writeUTF(getSubChannel());
        output.writeUTF(lines.get(0).replace("%this_parse%", gotParsed));
        output.writeUTF(lines.get(1).replace("%this_parsed%", parsedAs));
        output.writeUTF(lines.get(2).replace("%this_user_uuid%", user.getUUID()));

        return new ProxyMessageOut(SLAPI.getApiChannel(), getSubChannel(), output.toByteArray());
    }

    public static ReturnedMessage unbuild(ProxyMessageIn messageIn) {
        List<StreamlineUser> users = new ArrayList<>();
        ByteArrayDataInput input = ByteStreams.newDataInput(messageIn.getMessages());
        if (! messageIn.getSubChannel().equals(input.readUTF())) {
            SLAPI.getInstance().getMessenger().logWarning("Data mis-match on ProxyMessageIn for '" + ReturnParseMessageBuilder.class.getSimpleName() + "'. Continuing anyway...");
        }

        List<String> l = new ArrayList<>();
        l.add(input.readUTF());
        l.add(input.readUTF());
        l.add(input.readUTF());

        String parse = ProxyMessageHelper.extrapolate(l.get(0)).value;
        String parsed = ProxyMessageHelper.extrapolate(l.get(1)).value;
        String uuid = ProxyMessageHelper.extrapolate(l.get(2)).value;

        return new ReturnedMessage(parsed, parsed, uuid);
    }

    public record ReturnedMessage(String gotParsed, String parsedAs, String uuid) {

    }
}
