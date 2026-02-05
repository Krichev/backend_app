package com.my.challenger.service.vibration;

import com.my.challenger.dto.vibration.*;
import com.my.challenger.entity.enums.VibrationSessionStatus;
import com.my.challenger.entity.vibration.VibrationGameSession;
import com.my.challenger.entity.vibration.VibrationSessionQuestion;
import com.my.challenger.entity.vibration.VibrationSessionQuestionId;
import com.my.challenger.entity.vibration.VibrationSong;
import com.my.challenger.exception.BadRequestException;
import com.my.challenger.exception.SessionNotFoundException;
import com.my.challenger.exception.SongNotFoundException;
import com.my.challenger.mapper.VibrationQuizMapper;
import com.my.challenger.repository.vibration.VibrationGameSessionRepository;
import com.my.challenger.repository.vibration.VibrationSessionQuestionRepository;
import com.my.challenger.repository.vibration.VibrationSongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VibrationQuizService {

    private final VibrationGameSessionRepository sessionRepository;
    private final VibrationSongRepository songRepository;
    private final VibrationSessionQuestionRepository sessionQuestionRepository;
    private final VibrationQuizMapper mapper = VibrationQuizMapper.INSTANCE;
    private final VibrationLeaderboardService leaderboardService;

    public GameSessionDTO startSession(String userId, StartGameSessionRequest request) {
        // 1. Fetch random songs
        List<VibrationSong> songs;
        if (request.getSongIds() != null && !request.getSongIds().isEmpty()) {
            songs = songRepository.findAllById(request.getSongIds());
        } else {
            songs = songRepository.findRandomSongs(
                    request.getQuestionCount(),
                    request.getDifficulty().name(),
                    (request.getCategories() != null && !request.getCategories().isEmpty()) ? request.getCategories().get(0) : null,
                    null
            );
        }

        if (songs.isEmpty()) {
            throw new BadRequestException("No songs found for criteria");
        }

        // 2. Create session entity
        VibrationGameSession session = VibrationGameSession.builder()
                .userId(userId)
                .difficulty(request.getDifficulty())
                .questionCount(songs.size())
                .maxReplaysPerQuestion(request.getMaxReplaysPerQuestion() != null ? request.getMaxReplaysPerQuestion() : 3)
                .guessTimeLimitSeconds(request.getGuessTimeLimitSeconds() != null ? request.getGuessTimeLimitSeconds() : 30)
                .status(VibrationSessionStatus.ACTIVE)
                .currentQuestionIndex(0)
                .totalScore(0)
                .correctAnswers(0)
                .build();

        session = sessionRepository.save(session);

        // 3. Link songs to session
        List<VibrationSessionQuestion> sessionQuestions = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) {
            VibrationSong song = songs.get(i);
            VibrationSessionQuestion sq = VibrationSessionQuestion.builder()
                    .id(new VibrationSessionQuestionId(session.getId(), song.getId()))
                    .session(session)
                    .song(song)
                    .questionOrder(i)
                    .replaysUsed(0)
                    .points_earned(0)
                    .build();
            sessionQuestions.add(sq);
        }
        sessionQuestionRepository.saveAll(sessionQuestions);
        session.setQuestions(sessionQuestions);

        return mapper.toDTO(session);
    }

    public SubmitAnswerResponse submitAnswer(String userId, SubmitAnswerRequest request) {
        VibrationGameSession session = sessionRepository.findByIdAndUserId(request.getSessionId(), userId)
                .orElseThrow(() -> new SessionNotFoundException(request.getSessionId()));

        if (session.getStatus() != VibrationSessionStatus.ACTIVE) {
            throw new BadRequestException("Session is not active");
        }

        VibrationSessionQuestion sq = sessionQuestionRepository.findBySessionIdAndSongId(request.getSessionId(), request.getSongId())
                .orElseThrow(() -> new BadRequestException("Question not found in session"));

        if (sq.getAnsweredAt() != null) {
            throw new BadRequestException("Question already answered");
        }

        // Check answer
        boolean isCorrect = sq.getSong().getSongTitle().equalsIgnoreCase(request.getAnswer());
        int points = 0;
        if (isCorrect) {
            // Simple scoring logic: 100 points base - time penalty - replay penalty
            points = Math.max(10, 100 - (request.getResponseTimeMs() / 1000) - (request.getReplaysUsed() * 20));
            session.setCorrectAnswers(session.getCorrectAnswers() + 1);
            session.setTotalScore(session.getTotalScore() + points);
            
            songRepository.incrementCorrectGuesses(sq.getSong().getId());
        }
        
        songRepository.incrementPlayCount(sq.getSong().getId());

        sq.setIsCorrect(isCorrect);
        sq.setSelectedAnswer(request.getAnswer());
        sq.setResponseTimeMs(request.getResponseTimeMs());
        sq.setReplaysUsed(request.getReplaysUsed());
        sq.setPoints_earned(points);
        sq.setAnsweredAt(LocalDateTime.now());

        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);
        
        boolean isComplete = session.getCurrentQuestionIndex() >= session.getQuestionCount();
        if (isComplete) {
            session.setStatus(VibrationSessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            // Update leaderboard
            leaderboardService.updateLeaderboard(session);
        }

        sessionRepository.save(session);
        sessionQuestionRepository.save(sq);

        return SubmitAnswerResponse.builder()
                .isCorrect(isCorrect)
                .correctAnswer(sq.getSong().getSongTitle())
                .pointsEarned(points)
                .totalScore(session.getTotalScore())
                .correctAnswers(session.getCorrectAnswers())
                .currentQuestionIndex(session.getCurrentQuestionIndex())
                .isGameComplete(isComplete)
                .build();
    }

    public GameResultsDTO completeSession(UUID sessionId, String userId) {
        VibrationGameSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (session.getStatus() == VibrationSessionStatus.ACTIVE) {
            session.setStatus(VibrationSessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);
            leaderboardService.updateLeaderboard(session);
        }

        List<RoundResultDTO> rounds = session.getQuestions().stream()
                .map(mapper::toRoundResultDTO)
                .collect(Collectors.toList());

        double accuracy = session.getQuestionCount() > 0 ? (double) session.getCorrectAnswers() / session.getQuestionCount() * 100 : 0;
        
        return GameResultsDTO.builder()
                .sessionId(session.getId())
                .totalScore(session.getTotalScore())
                .correctAnswers(session.getCorrectAnswers())
                .totalQuestions(session.getQuestionCount())
                .accuracyPercent(accuracy)
                .difficulty(session.getDifficulty())
                .rounds(rounds)
                .build();
    }

    public void abandonSession(UUID sessionId, String userId) {
        VibrationGameSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        session.setStatus(VibrationSessionStatus.ABANDONED);
        sessionRepository.save(session);
    }
    
    public GameSessionDTO getActiveSession(String userId) {
        return sessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, VibrationSessionStatus.ACTIVE)
                .map(mapper::toDTO)
                .orElse(null);
    }
}
