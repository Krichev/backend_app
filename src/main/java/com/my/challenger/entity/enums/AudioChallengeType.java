package com.my.challenger.entity.enums;

/**
 * Types of audio-based challenges
 */
public enum AudioChallengeType {
    /**
     * User creates their own rhythm pattern
     * Evaluated on consistency and creativity
     */
    RHYTHM_CREATION,

    /**
     * User repeats a rhythm pattern from the reference audio
     * Evaluated on timing accuracy
     */
    RHYTHM_REPEAT,

    /**
     * User makes sounds matching the reference audio
     * Evaluated on pitch and timbre similarity
     */
    SOUND_MATCH,

    /**
     * User sings along with the reference track
     * Full karaoke scoring (pitch, rhythm, voice quality)
     */
    SINGING;

    /**
     * Check if this challenge type requires reference audio
     */
    public boolean requiresReferenceAudio() {
        return this != RHYTHM_CREATION;
    }

    /**
     * Check if this challenge type uses pitch scoring
     */
    public boolean usesPitchScoring() {
        return this == SOUND_MATCH || this == SINGING;
    }

    /**
     * Check if this challenge type uses rhythm scoring
     */
    public boolean usesRhythmScoring() {
        return this == RHYTHM_REPEAT || this == RHYTHM_CREATION || this == SINGING;
    }

    /**
     * Check if this challenge type uses voice similarity scoring
     */
    public boolean usesVoiceScoring() {
        return this == SOUND_MATCH || this == SINGING;
    }

    /**
     * Get scoring weights for this challenge type
     * @return array [pitchWeight, rhythmWeight, voiceWeight]
     */
    public double[] getScoringWeights() {
        switch (this) {
            case RHYTHM_CREATION:
                return new double[]{0.0, 1.0, 0.0};
            case RHYTHM_REPEAT:
                return new double[]{0.0, 0.9, 0.1};
            case SOUND_MATCH:
                return new double[]{0.5, 0.1, 0.4};
            case SINGING:
                return new double[]{0.5, 0.3, 0.2};
            default:
                return new double[]{0.33, 0.33, 0.34};
        }
    }
}
