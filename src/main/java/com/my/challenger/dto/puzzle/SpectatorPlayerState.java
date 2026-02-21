package com.my.challenger.dto.puzzle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpectatorPlayerState {
    private String username;
    private List<PiecePlacement> boardState;    // Current piece positions
    private boolean hasAnswered;
}
