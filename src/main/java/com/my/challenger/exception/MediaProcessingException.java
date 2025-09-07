package com.my.challenger.exception;

/**
 * Custom exception for errors that occur during media processing.
 */
public class MediaProcessingException extends RuntimeException {

    public MediaProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MediaProcessingException(String message) {
        super(message);
    }
}