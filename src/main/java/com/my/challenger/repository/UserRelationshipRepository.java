// UserRelationshipRepository.java
package com.my.challenger.repository;

import com.my.challenger.entity.UserRelationship;
import com.my.challenger.entity.enums.RelationshipStatus;
import com.my.challenger.entity.enums.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRelationshipRepository extends JpaRepository<UserRelationship, Long> {
    
    /**
     * Find relationship between two users (bidirectional)
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "(ur.user.id = :userId AND ur.relatedUser.id = :relatedUserId) OR " +
           "(ur.user.id = :relatedUserId AND ur.relatedUser.id = :userId)")
    Optional<UserRelationship> findBetweenUsers(@Param("userId") Long userId, 
                                                 @Param("relatedUserId") Long relatedUserId);
    
    /**
     * Find all relationships for a user
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "ur.user.id = :userId OR ur.relatedUser.id = :userId")
    List<UserRelationship> findAllForUser(@Param("userId") Long userId);
    
    /**
     * Find accepted friends and family for a user
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "(ur.user.id = :userId OR ur.relatedUser.id = :userId) " +
           "AND ur.status = :status " +
           "AND ur.relationshipType IN :types")
    List<UserRelationship> findAcceptedRelationships(@Param("userId") Long userId,
                                                      @Param("status") RelationshipStatus status,
                                                      @Param("types") List<RelationshipType> types);
    
    /**
     * Check if two users have an accepted relationship
     */
    @Query("SELECT CASE WHEN COUNT(ur) > 0 THEN true ELSE false END " +
           "FROM UserRelationship ur WHERE " +
           "((ur.user.id = :userId AND ur.relatedUser.id = :relatedUserId) OR " +
           " (ur.user.id = :relatedUserId AND ur.relatedUser.id = :userId)) " +
           "AND ur.status = 'ACCEPTED' " +
           "AND ur.relationshipType IN ('FRIEND', 'FAMILY')")
    boolean areUsersConnected(@Param("userId") Long userId, 
                              @Param("relatedUserId") Long relatedUserId);
    
    /**
     * Get all friend and family IDs for a user
     */
    @Query("SELECT CASE WHEN ur.user.id = :userId THEN ur.relatedUser.id ELSE ur.user.id END " +
           "FROM UserRelationship ur WHERE " +
           "(ur.user.id = :userId OR ur.relatedUser.id = :userId) " +
           "AND ur.status = 'ACCEPTED' " +
           "AND ur.relationshipType IN ('FRIEND', 'FAMILY')")
    List<Long> findConnectedUserIds(@Param("userId") Long userId);
    
    /**
     * Find pending relationship requests for a user
     */
    @Query("SELECT ur FROM UserRelationship ur WHERE " +
           "ur.relatedUser.id = :userId " +
           "AND ur.status = 'PENDING'")
    List<UserRelationship> findPendingRequestsForUser(@Param("userId") Long userId);
}