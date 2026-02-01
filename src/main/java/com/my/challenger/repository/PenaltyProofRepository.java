package com.my.challenger.repository;

import com.my.challenger.entity.penalty.PenaltyProof;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PenaltyProofRepository extends JpaRepository<PenaltyProof, Long> {

    List<PenaltyProof> findByPenaltyId(Long penaltyId);

    List<PenaltyProof> findByPenaltyIdOrderBySubmittedAtDesc(Long penaltyId);

    Optional<PenaltyProof> findByPenaltyIdAndApprovedTrue(Long penaltyId);
}
