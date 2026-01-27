package com.my.challenger.entity.enums;

public enum MediaSourceType {
    UPLOADED,        // File uploaded to MinIO/S3
    EXTERNAL_URL,    // Direct video/audio URL
    YOUTUBE,         // YouTube video with ID extraction
    VIMEO,           // Vimeo video
    SOUNDCLOUD;      // SoundCloud audio

    public boolean isExternal() {
        return this != UPLOADED;
    }

    public boolean requiresUrlExtraction() {
        return this == YOUTUBE || this == VIMEO;
    }
}
