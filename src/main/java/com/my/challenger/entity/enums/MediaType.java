package com.my.challenger.entity.enums;

/**
 * Types of media files supported
 */
public enum MediaType {
    /**
     * Image files (JPEG, PNG, GIF, WebP)
     */
    image,

    /**
     * Video files (MP4, MOV, AVI)
     */
    video,

    /**
     * Audio files (MP3, WAV, AAC, M4A)
     */
    audio,

    /**
     * Document files (PDF, DOC, etc.)
     */
    document,

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
        return this == image || this == video;
    }

    /**
     * Check if this media type has duration
     */
    public boolean hasDuration() {
        return this == video || this == audio;
    }

    /**
     * Get allowed MIME types for this media type
     */
    public String[] getAllowedMimeTypes() {
        switch (this) {
            case image:
                return new String[]{"image/jpeg", "image/png", "image/gif", "image/webp"};
            case video:
                return new String[]{"video/mp4", "video/mov", "video/avi", "video/quicktime"};
            case audio:
                return new String[]{"audio/mp3", "audio/wav", "audio/aac", "audio/m4a", "audio/ogg"};
            case document:
                return new String[]{"application/pdf", "application/msword", "text/plain"};
            default:
                return new String[]{};
        }
    }
}
