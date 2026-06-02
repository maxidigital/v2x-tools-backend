package main.loader;

import a.MessageId;

/** No definition exists in the repo for this message (after a cache-miss lookup). */
public class MessageNotAvailableException extends RuntimeException {
    public MessageNotAvailableException(MessageId mid) {
        super("No message definition available for " + mid.getStringId());
    }
}
