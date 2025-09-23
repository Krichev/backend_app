package com.my.challenger.entity.enums;


/**
 * Types of quiz questions based on content type
 */
public enum QuestionType {
    /**
     * Text-only questions
     */
    TEXT,

    /**
     * Questions with images
     */
    IMAGE,

    /**
     * Questions with audio files
     */
    AUDIO,

    /**
     * Questions with video content
     */
    VIDEO,

    /**
     * Questions with mixed multimedia content
     */
    MULTIMEDIA;

    /**
     * Check if this question type supports media content
     */
    public boolean hasMedia() {
        return this != TEXT;
    }

    /**
     * Check if this question type is visual
     */
    public boolean isVisual() {
        return this == IMAGE || this == VIDEO || this == MULTIMEDIA;
    }

    /**
     * Check if this question type has audio
     */
    public boolean hasAudio() {
        return this == AUDIO || this == VIDEO || this == MULTIMEDIA;
    }
}