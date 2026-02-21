package com.my.challenger.entity.puzzle;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.PuzzleGameMode;
import com.my.challenger.entity.enums.PuzzleSessionStatus;
import com.my.challenger.entity.enums.QuizDifficulty;
import com.my.challenger.entity.quiz.QuizSession;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "puzzle_games")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuzzleGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_session_id")
    private QuizSession quizSession;

    @Column(name = "room_code", length = 10)
    private String roomCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_image_media_id", nullable = false)
    private MediaFile sourceImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", nullable = false, columnDefinition = "puzzle_game_mode")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PuzzleGameMode gameMode;

    @Column(name = "grid_rows", nullable = false)
    @Builder.Default
    private int gridRows = 3;

    @Column(name = "grid_cols", nullable = false)
    @Builder.Default
    private int gridCols = 3;

    @Column(name = "total_pieces", nullable = false)
    private int totalPieces;

    @Column(name = "answer", nullable = false, length = 500)
    private String answer;

    @Column(name = "answer_aliases", columnDefinition = "TEXT")
    private String answerAliases; // Store as JSON array string

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, columnDefinition = "quiz_difficulty")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private QuizDifficulty difficulty = QuizDifficulty.MEDIUM;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @Column(name = "hint_text", length = 500)
    private String hintText;

    @Column(name = "hint_available_after_seconds")
    private Integer hintAvailableAfterSeconds;

    @Column(name = "enable_ai_validation")
    @Builder.Default
    private boolean enableAiValidation = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "puzzle_session_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private PuzzleSessionStatus status = PuzzleSessionStatus.CREATED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "puzzleGame", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PuzzlePiece> pieces = new ArrayList<>();

    @OneToMany(mappedBy = "puzzleGame", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PuzzleParticipant> participants = new ArrayList<>();
}
