package com.my.challenger.repository;

import com.my.challenger.entity.competitive.CompetitiveMatchInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompetitiveMatchInvitationRepository extends JpaRepository<CompetitiveMatchInvitation, Long> {

    List<CompetitiveMatchInvitation> findByInviteeIdAndStatus(Long inviteeId, String status);

    List<CompetitiveMatchInvitation> findByMatchId(Long matchId);
}
