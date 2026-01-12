package com.my.challenger.entity.enums;

public enum ResultSharingPolicy {
    CREATOR_ONLY,       // Only quiz creator sees all results
    PARTICIPANTS_ONLY,  // Each participant sees only their own result
    ALL_PARTICIPANTS,   // All participants can see everyone's results  
    PUBLIC,             // Anyone with quiz access can see results
    NONE                // Results hidden (except to participant themselves)
}
