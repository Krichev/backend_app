package com.my.challenger.entity.puzzle;

import com.my.challenger.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "puzzle_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"puzzle_game_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuzzleParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puzzle_game_id", nullable = false)
    private PuzzleGame puzzleGame;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "assigned_piece_ids", columnDefinition = "TEXT")
    private String assignedPieceIds; // JSON array of piece IDs

    @Column(name = "current_board_state", columnDefinition = "TEXT")
    private String currentBoardState; // JSON array of piece positions

    @Column(name = "text_answer", length = 500)
    private String textAnswer;

    @Column(name = "answer_correct")
    @Builder.Default
    private boolean answerCorrect = false;

    @Column(name = "answer_submitted_at")
    private LocalDateTime answerSubmittedAt;

    @Column(name = "pieces_placed_correctly")
    @Builder.Default
    private int piecesPlacedCorrectly = 0;

    @Column(name = "total_moves")
    @Builder.Default
    private int totalMoves = 0;

    @Column(name = "score")
    @Builder.Default
    private int score = 0;

    @Column(name = "completion_time_ms")
    private Long completionTimeMs;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
}
