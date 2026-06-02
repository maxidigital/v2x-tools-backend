package main.loader;

/** No definition exists in the repo for this message (after a cache-miss lookup). */
public class MessageNotAvailableException extends RuntimeException {
    public MessageNotAvailableException(int messageId, int protocolVersion) {
        super("no definition for message " + messageId + ":" + protocolVersion);
    }
}
