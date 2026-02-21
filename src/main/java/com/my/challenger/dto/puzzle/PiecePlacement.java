package com.my.challenger.dto.puzzle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PiecePlacement {
    private int pieceIndex;
    private int currentRow;
    private int currentCol;
}
