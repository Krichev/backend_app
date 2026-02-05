package com.my.challenger.entity.vibration;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vibration_session_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VibrationSessionQuestion {

    @EmbeddedId
    private VibrationSessionQuestionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sessionId")
    @JoinColumn(name = "session_id")
    private VibrationGameSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("songId")
    @JoinColumn(name = "song_id")
    private VibrationSong song;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(name = "selected_answer")
    private String selectedAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "replays_used")
    @Builder.Default
    private Integer replaysUsed = 0;

    @Column(name = "points_earned")
    @Builder.Default
    private Integer points_earned = 0;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
}
