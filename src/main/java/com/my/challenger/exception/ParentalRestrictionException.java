package com.my.challenger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ParentalRestrictionException extends RuntimeException {
    public ParentalRestrictionException(String message) {
        super(message);
    }
}
