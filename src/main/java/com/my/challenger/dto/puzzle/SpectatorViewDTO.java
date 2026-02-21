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
public class SpectatorViewDTO {
    private PuzzleGameDTO game;
    private List<SpectatorPlayerState> players;  // All players' board states for TV display
}
