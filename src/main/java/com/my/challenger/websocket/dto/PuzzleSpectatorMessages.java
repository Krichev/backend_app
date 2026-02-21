package com.my.challenger.websocket.dto;

import com.my.challenger.entity.enums.PuzzleEdgeType;
import com.my.challenger.entity.enums.PuzzleGameMode;
import com.my.challenger.entity.enums.PuzzleSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class PuzzleSpectatorMessages {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PuzzleStateMessage {
        private String roomCode;
        private Long puzzleGameId;
        private PuzzleSessionStatus phase;
        private PuzzleGameMode gameMode;
        private int gridRows;
        private int gridCols;
        private Integer timeLeftSeconds;
        private int totalPieces;
        private int teamCorrectCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PiecePlacedMessage {
        private Long userId;
        private String username;
        private int pieceIndex;
        private int row;
        private int col;
        private boolean correct;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerBoardSnapshot {
        private Long userId;
        private String username;
        private List<PiecePlacement> boardState;
        private int piecesPlacedCorrectly;
        private boolean hasAnswered;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PiecePlacement {
        private int pieceIndex;
        private int currentRow;
        private int currentCol;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpectatorSnapshot {
        private PuzzleStateMessage state;
        private List<PlayerBoardSnapshot> players;
        private List<PieceMetadata> pieces; // Only sent once or on request
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PieceMetadata {
        private int pieceIndex;
        private String imageUrl;
        private String svgClipPath;
        private PuzzleEdgeType edgeTop;
        private PuzzleEdgeType edgeRight;
        private PuzzleEdgeType edgeBottom;
        private PuzzleEdgeType edgeLeft;
        private int widthPx;
        private int heightPx;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerSubmittedMessage {
        private Long userId;
        private String username;
        private boolean correct;
        private int score;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PuzzleCompletedMessage {
        private String sourceImageUrl;
        private String correctAnswer;
        private List<PlayerResult> leaderboard;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerResult {
        private String username;
        private int score;
        private int piecesPlacedCorrectly;
        private int totalMoves;
        private Long completionTimeMs;
        private int rank;
    }
}
