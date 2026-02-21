package com.my.challenger.dto.puzzle;

import com.my.challenger.entity.puzzle.PuzzleGame;
import com.my.challenger.entity.puzzle.PuzzleParticipant;
import com.my.challenger.entity.puzzle.PuzzlePiece;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PuzzleDtoMapper {

    public static PuzzleGameDTO toDTO(PuzzleGame game) {
        if (game == null) return null;
        
        return PuzzleGameDTO.builder()
                .id(game.getId())
                .challengeId(game.getChallenge() != null ? game.getChallenge().getId() : null)
                .gameMode(game.getGameMode().name())
                .gridRows(game.getGridRows())
                .gridCols(game.getGridCols())
                .totalPieces(game.getTotalPieces())
                .difficulty(game.getDifficulty().name())
                .status(game.getStatus().name())
                .timeLimitSeconds(game.getTimeLimitSeconds())
                .hintText(game.getHintText())
                .participantCount(game.getParticipants() != null ? game.getParticipants().size() : 0)
                .createdAt(game.getCreatedAt())
                .startedAt(game.getStartedAt())
                .build();
    }

    public static PuzzlePieceDTO toDTO(PuzzlePiece piece, String presignedUrl, boolean revealPosition) {
        if (piece == null) return null;

        return PuzzlePieceDTO.builder()
                .id(piece.getId())
                .pieceIndex(piece.getPieceIndex())
                .gridRow(revealPosition ? piece.getGridRow() : null)
                .gridCol(revealPosition ? piece.getGridCol() : null)
                .imageUrl(presignedUrl)
                .edgeTop(piece.getEdgeTop().name())
                .edgeRight(piece.getEdgeRight().name())
                .edgeBottom(piece.getEdgeBottom().name())
                .edgeLeft(piece.getEdgeLeft().name())
                .svgClipPath(piece.getSvgClipPath())
                .widthPx(piece.getWidthPx())
                .heightPx(piece.getHeightPx())
                .build();
    }

    public static PuzzleParticipantDTO toDTO(PuzzleParticipant participant) {
        if (participant == null) return null;

        return PuzzleParticipantDTO.builder()
                .userId(participant.getUser() != null ? participant.getUser().getId() : null)
                .username(participant.getUser() != null ? participant.getUser().getUsername() : null)
                .piecesPlacedCorrectly(participant.getPiecesPlacedCorrectly())
                .totalMoves(participant.getTotalMoves())
                .answerSubmitted(participant.getTextAnswer() != null)
                .answerCorrect(participant.isAnswerCorrect())
                .score(participant.getScore())
                .completionTimeMs(participant.getCompletionTimeMs())
                .build();
    }

    public static PuzzleGameStatusDTO toStatusDTO(PuzzleGame game) {
        if (game == null) return null;

        long elapsed = 0;
        if (game.getStartedAt() != null) {
            LocalDateTime end = game.getCompletedAt() != null ? game.getCompletedAt() : LocalDateTime.now();
            elapsed = Duration.between(game.getStartedAt(), end).toMillis();
        }

        return PuzzleGameStatusDTO.builder()
                .game(toDTO(game))
                .participants(game.getParticipants().stream()
                        .map(PuzzleDtoMapper::toDTO)
                        .collect(Collectors.toList()))
                .isStarted(game.getStartedAt() != null)
                .isCompleted(game.getCompletedAt() != null)
                .elapsedTimeMs(elapsed)
                .build();
    }
}
