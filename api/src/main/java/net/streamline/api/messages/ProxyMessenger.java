package net.streamline.api.messages;

public interface ProxyMessenger {
    void sendMessage(ProxyMessageOut message);

    void receiveMessage(ProxyMessageEvent event);
}
