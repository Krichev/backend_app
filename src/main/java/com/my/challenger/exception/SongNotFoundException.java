package com.my.challenger.exception;

public class SongNotFoundException extends ResourceNotFoundException {
    public SongNotFoundException(Long id) {
        super("Song not found with id: " + id);
    }
}
