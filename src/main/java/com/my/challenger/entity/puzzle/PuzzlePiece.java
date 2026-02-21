package com.my.challenger.entity.puzzle;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.PuzzleEdgeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "puzzle_pieces")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuzzlePiece {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puzzle_game_id", nullable = false)
    private PuzzleGame puzzleGame;

    @Column(name = "piece_index", nullable = false)
    private int pieceIndex;

    @Column(name = "grid_row", nullable = false)
    private int gridRow;

    @Column(name = "grid_col", nullable = false)
    private int gridCol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piece_image_media_id", nullable = false)
    private MediaFile pieceImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "edge_top", nullable = false, columnDefinition = "puzzle_edge_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PuzzleEdgeType edgeTop;

    @Enumerated(EnumType.STRING)
    @Column(name = "edge_right", nullable = false, columnDefinition = "puzzle_edge_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PuzzleEdgeType edgeRight;

    @Enumerated(EnumType.STRING)
    @Column(name = "edge_bottom", nullable = false, columnDefinition = "puzzle_edge_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PuzzleEdgeType edgeBottom;

    @Enumerated(EnumType.STRING)
    @Column(name = "edge_left", nullable = false, columnDefinition = "puzzle_edge_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PuzzleEdgeType edgeLeft;

    @Column(name = "svg_clip_path", nullable = false, columnDefinition = "TEXT")
    private String svgClipPath;

    @Column(name = "width_px", nullable = false)
    private int widthPx;

    @Column(name = "height_px", nullable = false)
    private int heightPx;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
