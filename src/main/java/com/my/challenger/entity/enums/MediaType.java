package com.my.challenger.entity.enums;

/**
 * Types of media files supported
 */
public enum MediaType {
    /**
     * Image files (JPEG, PNG, GIF, WebP)
     */
    IMAGE,

    /**
     * Video files (MP4, MOV, AVI)
     */
    VIDEO,

    /**
     * Audio files (MP3, WAV, AAC, M4A)
     */
    AUDIO,

    /**
     * Document files (PDF, DOC, etc.)
     */
    DOCUMENT,

    /**
     * Quiz question media specifically
     */
    QUIZ_QUESTION,

    /**
     * Avatar media specifically
     */
    AVATAR;

    /**
     * Check if this media type supports thumbnails
     */
    public boolean supportsThumbnails() {
        return this == IMAGE || this == VIDEO;
    }

    /**
     * Check if this media type has duration
     */
    public boolean hasDuration() {
        return this == VIDEO || this == AUDIO;
    }

    /**
     * Get allowed MIME types for this media type
     */
    public String[] getAllowedMimeTypes() {
        switch (this) {
            case IMAGE:
                return new String[]{"image/jpeg", "image/png", "image/gif", "image/webp"};
            case VIDEO:
                return new String[]{"video/mp4", "video/mov", "video/avi", "video/quicktime"};
            case AUDIO:
                return new String[]{"audio/mp3", "audio/wav", "audio/aac", "audio/m4a", "audio/ogg", "audio/mpeg", "audio/x-wav", "audio/webm"};
            case DOCUMENT:
                return new String[]{"application/pdf", "application/msword", "text/plain"};
            default:
                return new String[]{};
        }
    }
}
