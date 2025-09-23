package com.my.challenger.entity.enums;

/**
 * Processing status for uploaded media files
 */
public enum ProcessingStatus {
    /**
     * File uploaded but not yet processed
     */
    PENDING,

    /**
     * File is currently being processed (thumbnails, compression, etc.)
     */
    PROCESSING,

    /**
     * File has been successfully processed and is ready for use
     */
    COMPLETED,

    /**
     * File processing failed
     */
    FAILED;

    /**
     * Check if processing is complete
     */
    public boolean isComplete() {
        return this == COMPLETED || this == FAILED;
    }

    /**
     * Check if processing was successful
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
}
