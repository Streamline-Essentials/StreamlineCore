package net.streamline.api.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MatcherUtils;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ResourcePackMessageBuilder {
    @Getter
    private static final String subChannel = "resource-pack";

    @Getter
    private static final List<String> lines = List.of(
            "user_uuid=%this_user_uuid%;",
            "url=%this_url%;",
            "prompt=%this_prompt%;",
            "hash=%this_hash%;",
            "force=%this_force%;"
    );

    public static ProxyMessageOut build(StreamlineUser user, StreamlineResourcePack resourcePack) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        output.writeUTF(getSubChannel());
        output.writeUTF(lines.get(0).replace("%this_user_uuid%", user.getUUID()));
        output.writeUTF(lines.get(1).replace("%this_url%", resourcePack.getUrl()));
        output.writeUTF(lines.get(2).replace("%this_prompt%", resourcePack.getPrompt()));
        output.writeUTF(lines.get(3).replace("%this_hash%",  getBytes(resourcePack.getHash())));
        output.writeUTF(lines.get(4).replace("%this_force%", String.valueOf(resourcePack.isForce())));

        return new ProxyMessageOut(SLAPI.getApiChannel(), getSubChannel(), output.toByteArray());
    }

    public static SingleSet<String, StreamlineResourcePack> unbuild(ProxyMessageIn messageIn) {
        ByteArrayDataInput input = ByteStreams.newDataInput(messageIn.getMessages());
        if (! messageIn.getSubChannel().equals(input.readUTF())) {
            SLAPI.getInstance().getMessenger().logWarning("Data mis-match on ProxyMessageIn for '" + ResourcePackMessageBuilder.class.getSimpleName() + "'. Continuing anyway...");
        }

        List<String> l = new ArrayList<>();
        l.add(input.readUTF());
        l.add(input.readUTF());
        l.add(input.readUTF());
        l.add(input.readUTF());
        l.add(input.readUTF());

        String uuid = ProxyMessageHelper.extrapolate(l.get(0)).value;
        String url = ProxyMessageHelper.extrapolate(l.get(1)).value;
        String prompt = ProxyMessageHelper.extrapolate(l.get(2)).value;
        byte[] hash = DigestUtils.sha1(ProxyMessageHelper.extrapolate(l.get(3)).value);
        boolean force = Boolean.parseBoolean(ProxyMessageHelper.extrapolate(l.get(4)).value);

        return new SingleSet<>(uuid, new StreamlineResourcePack(url, hash, prompt, force));
    }

    public static String getBytes(byte[] bytes) {
        StringBuilder builder = new StringBuilder();

        AtomicInteger integer = new AtomicInteger(0);
        for (byte b : bytes) {
            if (integer.get() < bytes.length) {
                builder.append(b).append(",");
            } else {
                builder.append(b);
            }
            integer.incrementAndGet();
        }

        return builder.toString();
    }
}
