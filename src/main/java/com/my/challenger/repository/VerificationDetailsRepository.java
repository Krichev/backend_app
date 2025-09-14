package com.my.challenger.repository;

import com.my.challenger.entity.challenge.VerificationDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationDetailsRepository extends JpaRepository<VerificationDetails, Long> {
    
    List<VerificationDetails> findByChallengeId(Long challengeId);
    
    Optional<VerificationDetails> findByChallengeIdAndActivityType(Long challengeId, String activityType);
    
    @Query("SELECT vd FROM VerificationDetails vd WHERE vd.challenge.id = :challengeId AND vd.activityType = :activityType")
    Optional<VerificationDetails> findByChallenge_IdAndActivityType(@Param("challengeId") Long challengeId, 
                                                                    @Param("activityType") String activityType);
    
    void deleteByChallengeId(Long challengeId);
    
    boolean existsByChallengeId(Long challengeId);
}