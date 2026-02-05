package com.my.challenger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidInvitationStateException extends RuntimeException {
    public InvalidInvitationStateException(String message) {
        super(message);
    }
}
