package com.my.challenger.repository;

import com.my.challenger.entity.QuestInvitation;
import com.my.challenger.entity.enums.QuestInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuestInvitationRepository extends JpaRepository<QuestInvitation, Long> {
       
    // Find invitations received by a user
    @Query("SELECT qi FROM QuestInvitation qi WHERE qi.invitee.id = :userId AND qi.status IN :statuses ORDER BY qi.createdAt DESC")
    List<QuestInvitation> findByInviteeAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<QuestInvitationStatus> statuses);
       
    // Find invitations sent by a user
    @Query("SELECT qi FROM QuestInvitation qi WHERE qi.inviter.id = :userId ORDER BY qi.createdAt DESC")
    List<QuestInvitation> findByInviterId(@Param("userId") Long userId);
       
    // Check if already invited
    @Query("SELECT CASE WHEN COUNT(qi) > 0 THEN true ELSE false END FROM QuestInvitation qi WHERE qi.quest.id = :questId AND qi.invitee.id = :inviteeId AND qi.status IN :statuses")
    boolean existsByQuestIdAndInviteeIdAndStatusIn(@Param("questId") Long questId, @Param("inviteeId") Long inviteeId, @Param("statuses") List<QuestInvitationStatus> statuses);
       
    // Find expired pending invitations
    @Query("SELECT qi FROM QuestInvitation qi WHERE qi.status = 'PENDING' AND qi.expiresAt < :now")
    List<QuestInvitation> findExpiredInvitations(@Param("now") LocalDateTime now);
       
    // Count pending for user
    long countByInviteeIdAndStatus(Long inviteeId, QuestInvitationStatus status);
       
    // Find by quest
    List<QuestInvitation> findByQuestId(Long questId);
}
