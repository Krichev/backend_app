package com.my.challenger.dto.puzzle;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardStateUpdate {
    @NotNull(message = "Piece index is required")
    private Integer pieceIndex;
    
    @NotNull(message = "New row is required")
    private Integer newRow;
    
    @NotNull(message = "New column is required")
    private Integer newCol;
}
