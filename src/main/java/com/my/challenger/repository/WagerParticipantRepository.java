package com.my.challenger.repository;

import com.my.challenger.entity.enums.ParticipantWagerStatus;
import com.my.challenger.entity.wager.WagerParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WagerParticipantRepository extends JpaRepository<WagerParticipant, Long> {

    Optional<WagerParticipant> findByWagerIdAndUserId(Long wagerId, Long userId);

    List<WagerParticipant> findByWagerId(Long wagerId);

    List<WagerParticipant> findByUserId(Long userId);

    long countByWagerIdAndStatus(Long wagerId, ParticipantWagerStatus status);
}
