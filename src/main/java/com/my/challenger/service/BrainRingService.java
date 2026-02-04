package com.my.challenger.service;

import com.my.challenger.dto.quiz.BrainRingAnswerResponse;
import com.my.challenger.dto.quiz.BrainRingStateDTO;
import com.my.challenger.dto.quiz.BuzzResponse;
import com.my.challenger.entity.quiz.QuizRound;

import java.time.Instant;
import java.util.List;

public interface BrainRingService {
    BuzzResponse processBuzz(Long sessionId, Long roundId, Long userId, Instant clientTimestamp);
    BrainRingAnswerResponse submitAnswer(Long sessionId, Long roundId, Long userId, String answer);
    BrainRingStateDTO getRoundState(Long sessionId, Long roundId);
    void initializeRoundState(QuizRound round);
    boolean isPlayerLockedOut(Long roundId, Long userId);
}
