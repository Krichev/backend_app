package com.my.challenger.entity.quiz;

import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.ParticipantConsentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_participant_settings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"challenge_id", "user_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizParticipantSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_consent_status")
    @Builder.Default
    private ParticipantConsentStatus resultConsentStatus = ParticipantConsentStatus.NOT_ASKED;

    @Column(name = "consent_requested_at")
    private LocalDateTime consentRequestedAt;

    @Column(name = "consent_responded_at")
    private LocalDateTime consentRespondedAt;

    @Column(name = "attempts_used")
    @Builder.Default
    private Integer attemptsUsed = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "best_score", precision = 5, scale = 2)
    private BigDecimal bestScore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void recordAttempt(BigDecimal score) {
        this.attemptsUsed++;
        this.lastAttemptAt = LocalDateTime.now();
        if (this.bestScore == null || score.compareTo(this.bestScore) > 0) {
            this.bestScore = score;
        }
    }

    public void grantConsent() {
        this.resultConsentStatus = ParticipantConsentStatus.GRANTED;
        this.consentRespondedAt = LocalDateTime.now();
    }

    public void denyConsent() {
        this.resultConsentStatus = ParticipantConsentStatus.DENIED;
        this.consentRespondedAt = LocalDateTime.now();
    }
}
