package com.my.challenger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.ACCEPTED)
public class PendingParentalApprovalException extends RuntimeException {
    public PendingParentalApprovalException(String message) {
        super(message);
    }
}
