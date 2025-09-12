package com.my.challenger.entity.enums;

/**
 * Enumeration for different types of photos in the system
 */
public enum PhotoType {
    AVATAR("Avatar/Profile Picture"),
    QUIZ_QUESTION("Quiz Question Image"),
    CHALLENGE_COVER("Challenge Cover Image"),
    TASK_VERIFICATION("Task Verification Photo"),
    GENERAL("General Photo"),
    THUMBNAIL("Thumbnail Image"),
    BACKGROUND("Background Image"),
    BANNER("Banner Image"),
    GALLERY("Gallery Image"),
    DOCUMENT("Document Image");

    private final String description;

    PhotoType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this photo type should be publicly accessible
     */
    public boolean isPublicAccessible() {
        return this == AVATAR || this == QUIZ_QUESTION || this == CHALLENGE_COVER || this == BANNER;
    }

    /**
     * Check if this photo type requires resizing
     */
    public boolean requiresResizing() {
        return this == AVATAR || this == THUMBNAIL || this == QUIZ_QUESTION;
    }

    /**
     * Get the maximum dimensions for this photo type
     */
    public int[] getMaxDimensions() {
        switch (this) {
            case AVATAR:
                return new int[]{400, 400};
            case THUMBNAIL:
                return new int[]{150, 150};
            case QUIZ_QUESTION:
                return new int[]{800, 600};
            case BANNER:
                return new int[]{1200, 400};
            default:
                return new int[]{2048, 2048};
        }
    }
}