package com.my.challenger.repository;

import com.my.challenger.entity.parental.ParentalLink;
import com.my.challenger.entity.parental.ParentalLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParentalLinkRepository extends JpaRepository<ParentalLink, Long> {

    List<ParentalLink> findByParentIdAndStatus(Long parentId, ParentalLinkStatus status);

    List<ParentalLink> findByChildIdAndStatus(Long childId, ParentalLinkStatus status);

    Optional<ParentalLink> findByParentIdAndChildId(Long parentId, Long childId);

    @Query("SELECT pl FROM ParentalLink pl WHERE pl.child.id = :childId AND pl.status = 'ACTIVE'")
    List<ParentalLink> findActiveParentsForChild(Long childId);

    @Query("SELECT pl FROM ParentalLink pl WHERE pl.parent.id = :parentId AND pl.status = 'ACTIVE'")
    List<ParentalLink> findActiveChildrenForParent(Long parentId);

    boolean existsByParentIdAndChildIdAndStatus(Long parentId, Long childId, ParentalLinkStatus status);
}
