package com.my.challenger.entity.enums;

/**
 * Enumeration of question types supported in the quiz system
 */
public enum QuestionType {
    /**
     * Text-only question (traditional format)
     */
    TEXT,

    /**
     * Question with an associated image
     */
    IMAGE,

    /**
     * Question with an associated video file
     */
    VIDEO,

    /**
     * Question with an associated audio file
     */
    AUDIO;

    /**
     * Check if this question type requires media
     */
    public boolean requiresMedia() {
        return this != TEXT;
    }

    /**
     * Get the expected media type prefix for this question type
     */
    public String getMediaTypePrefix() {
        switch (this) {
            case IMAGE: return "image/";
            case VIDEO: return "video/";
            case AUDIO: return "audio/";
            default: return null;
        }
    }

    /**
     * Determine question type from media MIME type
     */
    public static QuestionType fromMediaType(String mimeType) {
        if (mimeType == null) return TEXT;

        if (mimeType.startsWith("image/")) return IMAGE;
        if (mimeType.startsWith("video/")) return VIDEO;
        if (mimeType.startsWith("audio/")) return AUDIO;

        return TEXT;
    }
}