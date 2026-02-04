package com.my.challenger.repository;

import com.my.challenger.entity.UserParentalSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserParentalSettingsRepository extends JpaRepository<UserParentalSettings, Long> {

    Optional<UserParentalSettings> findByUserId(Long userId);

    @Query("SELECT ups FROM UserParentalSettings ups WHERE ups.parentUserId = :parentId")
    List<UserParentalSettings> findChildrenByParentId(@Param("parentId") Long parentId);

    @Query("SELECT COUNT(ups) FROM UserParentalSettings ups WHERE ups.parentUserId = :parentId")
    long countChildrenByParentId(@Param("parentId") Long parentId);

    boolean existsByUserIdAndIsChildAccountTrue(Long userId);

    @Query("SELECT ups FROM UserParentalSettings ups WHERE ups.isChildAccount = true AND ups.parentUserId IS NULL")
    List<UserParentalSettings> findOrphanedChildAccounts();
}
