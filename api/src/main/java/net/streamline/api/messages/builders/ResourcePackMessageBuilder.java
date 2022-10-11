package net.streamline.api.messages.builders;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.objects.StreamlineResourcePack;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import org.apache.commons.codec.binary.Hex;

import java.util.ArrayList;
import java.util.List;

public class ResourcePackMessageBuilder {
    @Getter
    private static final String subChannel = "resource-pack";

    public static ProxiedMessage build(StreamlinePlayer carrier, boolean isProxyOriginated, StreamlineUser user, StreamlineResourcePack resourcePack) {
        ProxiedMessage r = new ProxiedMessage(carrier, isProxyOriginated);

        r.setSubChannel(getSubChannel());
        r.write("user_uuid", user.getUuid());
        r.write("url", resourcePack.getUrl());
        r.write("prompt", resourcePack.getPrompt());
        r.write("hash", Hex.encodeHexString(resourcePack.getHash()));
        r.write("force", String.valueOf(resourcePack.isForce()));

        return r;
    }

    public static SingleSet<String, StreamlineResourcePack> unbuild(ProxiedMessage messageIn) {
        if (! messageIn.getSubChannel().equals(getSubChannel())) {
            MessageUtils.logWarning("Data mis-match on ProxyMessageIn for '" + ResourcePackMessageBuilder.class.getSimpleName() + "'. Continuing anyway...");
        }

        String uuid = messageIn.getString("user_uuid");
        String url = messageIn.getString("url");
        String prompt = messageIn.getString("url");
        String unparsed = messageIn.getString("hash");
        byte[] hash;
        try {
            if (unparsed.equals("")) {
                hash = new byte[0];
            } else {
                hash = Hex.decodeHex(unparsed);
            }
        } catch (Exception e) {
            e.printStackTrace();
            hash = new byte[0];
        }
        boolean force = messageIn.getBoolean("force");

        return new SingleSet<>(uuid, new StreamlineResourcePack(url, hash, prompt, force));
    }
}
