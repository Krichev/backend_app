package com.my.challenger.exception;

import java.util.UUID;

public class SessionNotFoundException extends ResourceNotFoundException {
    public SessionNotFoundException(UUID id) {
        super("Session not found with id: " + id);
    }
}
