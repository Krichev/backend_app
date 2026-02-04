package com.my.challenger.repository;

import com.my.challenger.entity.parental.TimeExtensionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeExtensionRequestRepository extends JpaRepository<TimeExtensionRequest, Long> {
    List<TimeExtensionRequest> findByParentIdAndStatus(Long parentId, String status);
    List<TimeExtensionRequest> findByChildId(Long childId);
}
