package com.my.challenger.repository;

import com.my.challenger.entity.InvitationNegotiation;
import com.my.challenger.entity.enums.NegotiationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationNegotiationRepository extends JpaRepository<InvitationNegotiation, Long> {
    
    List<InvitationNegotiation> findByInvitationId(Long invitationId);
    
    @Query("SELECT ineg FROM InvitationNegotiation ineg WHERE ineg.invitation.id = :invitationId ORDER BY ineg.createdAt DESC LIMIT 1")
    Optional<InvitationNegotiation> findLatestByInvitationId(@Param("invitationId") Long invitationId);
    
    @Query("SELECT ineg FROM InvitationNegotiation ineg WHERE ineg.status = 'PROPOSED' AND ineg.invitation.inviter.id = :userId AND ineg.proposer.id != :userId OR ineg.status = 'PROPOSED' AND ineg.invitation.invitee.id = :userId AND ineg.proposer.id != :userId")
    List<InvitationNegotiation> findPendingByResponderId(@Param("userId") Long userId);
}
