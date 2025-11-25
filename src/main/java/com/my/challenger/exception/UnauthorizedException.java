// src/main/java/com/my/challenger/exception/UnauthorizedException.java
package com.my.challenger.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
