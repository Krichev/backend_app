package com.my.challenger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ScreenTimeLockedException extends RuntimeException {
    public ScreenTimeLockedException(String message) {
        super(message);
    }
}
