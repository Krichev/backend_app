package com.my.challenger.exception;

public class MediaProcessingException extends RuntimeException {

    public MediaProcessingException(String message) {
        super(message);
    }

    public MediaProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MediaProcessingException(Throwable cause) {
        super(cause);
    }
}