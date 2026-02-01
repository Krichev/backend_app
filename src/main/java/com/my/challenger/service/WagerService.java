package com.my.challenger.service;

import com.my.challenger.dto.wager.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WagerService {
    WagerDTO createWager(CreateWagerRequest request, Long creatorUserId);
    WagerDTO acceptWager(Long wagerId, Long userId);
    void declineWager(Long wagerId, Long userId);
    void cancelWager(Long wagerId, Long userId);
    WagerOutcomeDTO settleWager(Long wagerId);
    void updateParticipantScore(Long wagerId, Long userId, Integer score);
    int expireStaleWagers();
    List<WagerDTO> getWagersByChallenge(Long challengeId);
    List<WagerDTO> getActiveWagersByUser(Long userId);
    Page<WagerDTO> getWagerHistoryByUser(Long userId, Pageable pageable);
    WagerDTO getWagerById(Long id);
    void settleWagersForSession(Long sessionId, Long userId, Integer score);
}
