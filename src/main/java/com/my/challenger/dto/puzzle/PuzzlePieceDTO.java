package com.my.challenger.dto.puzzle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuzzlePieceDTO {
    private Long id;
    private int pieceIndex;
    private Integer gridRow;        // correct position (ONLY sent in INDIVIDUAL mode or after game ends)
    private Integer gridCol;        // correct position (ONLY sent in INDIVIDUAL mode or after game ends)
    private String imageUrl;    // presigned URL
    private String edgeTop;     // FLAT, TAB, BLANK
    private String edgeRight;
    private String edgeBottom;
    private String edgeLeft;
    private String svgClipPath; // SVG path data for rendering jigsaw shape on frontend
    private int widthPx;
    private int heightPx;
}
