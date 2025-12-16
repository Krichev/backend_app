package com.my.challenger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when audio segment configuration is invalid
 * (e.g., end time < start time, times exceed duration)
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidAudioSegmentException extends RuntimeException {

    public InvalidAudioSegmentException(String message) {
        super(message);
    }

    public InvalidAudioSegmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
