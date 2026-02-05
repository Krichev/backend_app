package com.my.challenger.repository;

import com.my.challenger.entity.competitive.CompetitiveMatch;
import com.my.challenger.entity.enums.CompetitiveMatchStatus;
import com.my.challenger.entity.enums.CompetitiveMatchType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompetitiveMatchRepository extends JpaRepository<CompetitiveMatch, Long> {

    @Query("SELECT m FROM CompetitiveMatch m WHERE m.player1.id = :userId OR m.player2.id = :userId")
    List<CompetitiveMatch> findByPlayer1IdOrPlayer2Id(@Param("userId") Long userId);

    @Query("SELECT m FROM CompetitiveMatch m WHERE " +
           "(m.player1.id = :userId OR m.player2.id = :userId) AND " +
           "m.status IN ('READY', 'IN_PROGRESS', 'ROUND_COMPLETE')")
    List<CompetitiveMatch> findActiveMatchesByUserId(@Param("userId") Long userId);

    List<CompetitiveMatch> findByStatusAndMatchType(CompetitiveMatchStatus status, CompetitiveMatchType matchType);

    @Query("SELECT m FROM CompetitiveMatch m WHERE m.player1.id = :userId AND m.status = 'WAITING_FOR_OPPONENT'")
    List<CompetitiveMatch> findPendingMatchesForUser(@Param("userId") Long userId);
}
