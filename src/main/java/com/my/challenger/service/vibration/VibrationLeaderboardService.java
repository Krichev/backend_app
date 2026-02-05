package com.my.challenger.service.vibration;

import com.my.challenger.dto.vibration.LeaderboardDTO;
import com.my.challenger.dto.vibration.LeaderboardEntryDTO;
import com.my.challenger.entity.enums.LeaderboardPeriod;
import com.my.challenger.entity.enums.VibrationDifficulty;
import com.my.challenger.entity.vibration.VibrationGameSession;
import com.my.challenger.entity.vibration.VibrationLeaderboard;
import com.my.challenger.repository.vibration.VibrationLeaderboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VibrationLeaderboardService {

    private final VibrationLeaderboardRepository repository;

    @Async
    @Transactional
    public void updateLeaderboard(VibrationGameSession session) {
        LocalDate now = LocalDate.now();
        
        // Update for different periods
        updateEntry(session, LeaderboardPeriod.DAILY, now, session.getDifficulty());
        updateEntry(session, LeaderboardPeriod.WEEKLY, now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), session.getDifficulty());
        updateEntry(session, LeaderboardPeriod.MONTHLY, now.with(TemporalAdjusters.firstDayOfMonth()), session.getDifficulty());
        updateEntry(session, LeaderboardPeriod.ALL_TIME, LocalDate.of(2000, 1, 1), session.getDifficulty());
        
        // Also update for "All Difficulties" (null difficulty)
        updateEntry(session, LeaderboardPeriod.DAILY, now, null);
        updateEntry(session, LeaderboardPeriod.WEEKLY, now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), null);
        updateEntry(session, LeaderboardPeriod.MONTHLY, now.with(TemporalAdjusters.firstDayOfMonth()), null);
        updateEntry(session, LeaderboardPeriod.ALL_TIME, LocalDate.of(2000, 1, 1), null);
    }

    private void updateEntry(VibrationGameSession session, LeaderboardPeriod period, LocalDate periodStart, VibrationDifficulty difficulty) {
        VibrationLeaderboard entry = repository.findByUserIdAndPeriodAndPeriodStartAndDifficulty(
                session.getUserId(), period, periodStart, difficulty)
                .orElse(VibrationLeaderboard.builder()
                        .userId(session.getUserId())
                        .period(period)
                        .periodStart(periodStart)
                        .difficulty(difficulty)
                        .totalScore(0)
                        .gamesPlayed(0)
                        .correctAnswers(0)
                        .totalQuestions(0)
                        .bestStreak(0)
                        .build());

        entry.setTotalScore(entry.getTotalScore() + session.getTotalScore());
        entry.setGamesPlayed(entry.getGamesPlayed() + 1);
        entry.setCorrectAnswers(entry.getCorrectAnswers() + session.getCorrectAnswers());
        entry.setTotalQuestions(entry.getTotalQuestions() + session.getQuestionCount());
        
        // Simplified streak update (not quite accurate without per-session streak info, but better than nothing)
        if (session.getCorrectAnswers() > entry.getBestStreak()) {
            entry.setBestStreak(session.getCorrectAnswers());
        }

        repository.save(entry);
    }

    public LeaderboardDTO getLeaderboard(LeaderboardPeriod period, VibrationDifficulty difficulty, int page, int size) {
        LocalDate periodStart = getPeriodStart(period);
        Page<VibrationLeaderboard> entries = repository.findByPeriodAndPeriodStartAndDifficultyOrderByTotalScoreDesc(
                period, periodStart, difficulty, PageRequest.of(page, size));

        List<LeaderboardEntryDTO> dtoList = entries.getContent().stream()
                .map(this::toEntryDTO)
                .collect(Collectors.toList());
        
        // Set ranks
        for (int i = 0; i < dtoList.size(); i++) {
            dtoList.get(i).setRank(page * size + i + 1);
        }

        return LeaderboardDTO.builder()
                .period(period)
                .difficulty(difficulty)
                .entries(dtoList)
                .page(page)
                .totalPages(entries.getTotalPages())
                .totalEntries(entries.getTotalElements())
                .build();
    }

    private LeaderboardEntryDTO toEntryDTO(VibrationLeaderboard entity) {
        return LeaderboardEntryDTO.builder()
                .userId(entity.getUserId())
                .username("User " + entity.getUserId()) // Should fetch from User service
                .totalScore(entity.getTotalScore())
                .gamesPlayed(entity.getGamesPlayed())
                .averageAccuracy(entity.getTotalQuestions() > 0 ? (double) entity.getCorrectAnswers() / entity.getTotalQuestions() * 100 : 0)
                .bestStreak(entity.getBestStreak())
                .build();
    }

    private LocalDate getPeriodStart(LeaderboardPeriod period) {
        LocalDate now = LocalDate.now();
        switch (period) {
            case DAILY: return now;
            case WEEKLY: return now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY: return now.with(TemporalAdjusters.firstDayOfMonth());
            case ALL_TIME: return LocalDate.of(2000, 1, 1);
            default: return now;
        }
    }
}
