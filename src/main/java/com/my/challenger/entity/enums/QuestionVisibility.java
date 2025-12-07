// QuestionVisibility.java
package com.my.challenger.entity.enums;

/**
 * Defines the access policy for user-created questions
 */
public enum QuestionVisibility {
    /**
     * Only the creator can see and use this question
     */
    PRIVATE,
    
    /**
     * Creator and their friends/family can see and use this question
     */
    FRIENDS_FAMILY,
    
    /**
     * Only accessible within the specific quiz/challenge where it was added
     */
    QUIZ_ONLY,

    /**
     * Submitted for public visibility but awaiting validation/approval
     * Visible to creator but not in public searches
     */
    PENDING_PUBLIC,

    /**
     * Available to everyone in question search and can be used by anyone
     */
    PUBLIC;
    
    /**
     * Get a user-friendly description
     */
    public String getDescription() {
        switch (this) {
            case PRIVATE:
                return "Only Me";
            case FRIENDS_FAMILY:
                return "Friends & Family";
            case QUIZ_ONLY:
                return "This Quiz Only";
            case PENDING_PUBLIC:
                return "Pending Public Approval";
            case PUBLIC:
                return "Everyone (Public)";
            default:
                return this.name();
        }
    }
    
    /**
     * Check if this visibility allows public access
     */
    public boolean isPubliclyAccessible() {
        return this == PUBLIC;
    }
    
    /**
     * Check if this requires relationship verification
     */
    public boolean requiresRelationship() {
        return this == FRIENDS_FAMILY;
    }
}


