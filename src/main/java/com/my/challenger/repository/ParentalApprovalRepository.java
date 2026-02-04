package com.my.challenger.repository;

import com.my.challenger.entity.parental.ParentalApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParentalApprovalRepository extends JpaRepository<ParentalApproval, Long> {
    List<ParentalApproval> findByParentIdAndStatus(Long parentId, String status);
    List<ParentalApproval> findByChildIdAndStatus(Long childId, String status);
}
