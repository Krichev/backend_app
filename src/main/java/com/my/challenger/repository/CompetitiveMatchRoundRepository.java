package com.my.challenger.repository;

import com.my.challenger.entity.competitive.CompetitiveMatchRound;
import com.my.challenger.entity.enums.CompetitiveRoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitiveMatchRoundRepository extends JpaRepository<CompetitiveMatchRound, Long> {

    List<CompetitiveMatchRound> findByMatchIdOrderByRoundNumberAsc(Long matchId);

    Optional<CompetitiveMatchRound> findFirstByMatchIdAndStatusNotOrderByRoundNumberAsc(Long matchId, CompetitiveRoundStatus status);
    
    // Convenience default method if needed, but keeping interface simple
}
