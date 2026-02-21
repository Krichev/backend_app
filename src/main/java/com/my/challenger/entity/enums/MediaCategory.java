package com.my.challenger.entity.enums;


/**
 * Categories of media files in the system
 */
public enum MediaCategory {
    /**
     * Media used for quiz questions (images, videos, audio)
     */
    QUIZ_QUESTION,

    /**
     * User avatar images or videos
     */
    AVATAR,

    /**
     * Media used as proof for challenge completion
     */
    CHALLENGE_PROOF,

    /**
     * System-level media files
     */
    SYSTEM,

    /**
     * Jigsaw puzzle pieces
     */
    PUZZLE_PIECE,

    /**
     * Temporary uploads that haven't been assigned yet
     */
    TEMPORARY;

    /**
     * Get the S3 folder prefix for this category
     */
    public String getS3Prefix() {
        return this.name().toLowerCase().replace('_', '-') + "/";
    }
}