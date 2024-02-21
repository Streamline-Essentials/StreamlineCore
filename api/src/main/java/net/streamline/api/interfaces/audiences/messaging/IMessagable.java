package net.streamline.api.interfaces.audiences.messaging;

public interface IMessagable {
    void sendMessage(String message);

    void sendMessageRaw(String message);
}
