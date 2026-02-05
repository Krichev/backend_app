package com.my.challenger.web.vibration;

import com.my.challenger.dto.vibration.*;
import com.my.challenger.entity.enums.LeaderboardPeriod;
import com.my.challenger.entity.enums.VibrationDifficulty;
import com.my.challenger.service.vibration.VibrationLeaderboardService;
import com.my.challenger.service.vibration.VibrationQuizService;
import com.my.challenger.service.vibration.VibrationSongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vibration-quiz")
@RequiredArgsConstructor
public class VibrationQuizController {

    private final VibrationQuizService quizService;
    private final VibrationSongService songService;
    private final VibrationLeaderboardService leaderboardService;

    // --- Songs ---

    @GetMapping("/songs")
    public ResponseEntity<Page<VibrationSongDTO>> getSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "playCount") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return ResponseEntity.ok(songService.getSongs(page, size, difficulty, category, sortBy, sortDirection));
    }

    @GetMapping("/songs/random")
    public ResponseEntity<List<VibrationSongDTO>> getRandomSongs(
            @RequestParam int count,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<Long> excludeIds) {
        return ResponseEntity.ok(songService.getRandomSongs(count, difficulty, category, excludeIds));
    }

    @PostMapping("/songs")
    public ResponseEntity<VibrationSongDTO> createSong(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateVibrationSongRequest request) {
        return ResponseEntity.ok(songService.createSong(request, userDetails.getUsername()));
    }

    @PutMapping("/songs/{id}")
    public ResponseEntity<VibrationSongDTO> updateSong(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVibrationSongRequest request) {
        return ResponseEntity.ok(songService.updateSong(id, request));
    }

    @DeleteMapping("/songs/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable Long id) {
        songService.deleteSong(id);
        return ResponseEntity.noContent().build();
    }

    // --- Sessions ---

    @PostMapping("/sessions")
    public ResponseEntity<GameSessionDTO> startSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody StartGameSessionRequest request) {
        return ResponseEntity.ok(quizService.startSession(userDetails.getUsername(), request));
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<GameSessionDTO> getActiveSession(@AuthenticationPrincipal UserDetails userDetails) {
        GameSessionDTO session = quizService.getActiveSession(userDetails.getUsername());
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{sessionId}/answer")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SubmitAnswerRequest request) {
        request.setSessionId(sessionId);
        return ResponseEntity.ok(quizService.submitAnswer(userDetails.getUsername(), request));
    }

    @PostMapping("/sessions/{sessionId}/complete")
    public ResponseEntity<GameResultsDTO> completeSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(quizService.completeSession(sessionId, userDetails.getUsername()));
    }

    @PostMapping("/sessions/{sessionId}/abandon")
    public ResponseEntity<Void> abandonSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        quizService.abandonSession(sessionId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    // --- Leaderboard ---

    @GetMapping("/leaderboard")
    public ResponseEntity<LeaderboardDTO> getLeaderboard(
            @RequestParam(defaultValue = "ALL_TIME") LeaderboardPeriod period,
            @RequestParam(required = false) VibrationDifficulty difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(period, difficulty, page, size));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDTO>> getCategories() {
        return ResponseEntity.ok(songService.getCategories());
    }

    // --- Admin ---

    @PostMapping("/admin/songs/{id}/approve")
    public ResponseEntity<Void> approveSong(@PathVariable Long id) {
        songService.approveSong(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/songs/{id}/reject")
    public ResponseEntity<Void> rejectSong(@PathVariable Long id) {
        songService.rejectSong(id);
        return ResponseEntity.ok().build();
    }
}
