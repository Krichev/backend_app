package com.my.challenger.entity.enums;

/**
 * Validation status for user-created topics and questions
 * Tracks the approval workflow for public content
 */
public enum ValidationStatus {
    /**
     * Just created, not submitted for review
     */
    DRAFT,

    /**
     * Submitted for validation, awaiting review
     */
    PENDING,

    /**
     * Validated and approved for public use
     */
    APPROVED,

    /**
     * Rejected by moderator
     */
    REJECTED,

    /**
     * Approved by AI (future use)
     */
    AUTO_APPROVED;

    /**
     * Get a user-friendly description
     */
    public String getDescription() {
        switch (this) {
            case DRAFT:
                return "Draft - Not Yet Submitted";
            case PENDING:
                return "Pending Review";
            case APPROVED:
                return "Approved";
            case REJECTED:
                return "Rejected";
            case AUTO_APPROVED:
                return "Auto-Approved";
            default:
                return this.name();
        }
    }

    /**
     * Check if this status indicates approved content
     */
    public boolean isApproved() {
        return this == APPROVED || this == AUTO_APPROVED;
    }

    /**
     * Check if this content is pending validation
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * Check if this content is rejected
     */
    public boolean isRejected() {
        return this == REJECTED;
    }

    /**
     * Check if this content is in draft state
     */
    public boolean isDraft() {
        return this == DRAFT;
    }
}
