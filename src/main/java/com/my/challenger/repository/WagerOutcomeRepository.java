package com.my.challenger.repository;

import com.my.challenger.entity.wager.WagerOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WagerOutcomeRepository extends JpaRepository<WagerOutcome, Long> {

    List<WagerOutcome> findByWagerId(Long wagerId);

    List<WagerOutcome> findByWinnerId(Long winnerId);

    List<WagerOutcome> findByLoserId(Long loserId);
}
