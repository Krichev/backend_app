package com.my.challenger.service;

import com.my.challenger.dto.parental.*;
import java.util.List;
import java.util.Map;

public interface ParentalControlService {

    // Link Management
    ParentalLinkDTO requestLink(Long parentId, Long childId);
    ParentalLinkDTO acceptLink(Long childId, Long linkId, String verificationCode);
    void rejectLink(Long childId, Long linkId);
    void revokeLink(Long userId, Long linkId);
    List<ParentalLinkDTO> getLinkedChildren(Long parentId);
    List<ParentalLinkDTO> getLinkedParents(Long childId);

    // Child Settings
    ChildSettingsDTO getChildSettings(Long parentId, Long childId);
    ChildSettingsDTO updateChildSettings(Long parentId, Long childId, UpdateChildSettingsRequest request);

    // Screen Time for Child (Parent View)
    ChildScreenTimeDTO getChildScreenTime(Long parentId, Long childId);
    void setChildDailyBudget(Long parentId, Long childId, int minutes);

    // Approvals
    List<ParentalApprovalDTO> getPendingApprovals(Long parentId);
    ParentalApprovalDTO approveRequest(Long parentId, Long approvalId, ApprovalResponseRequest request);
    ParentalApprovalDTO denyRequest(Long parentId, Long approvalId, ApprovalResponseRequest request);
    
    // Internal method for creating approvals
    void createApprovalRequest(Long childId, String approvalType, Long referenceId, String referenceType, Map<String, Object> details);

    // Time Extensions
    TimeExtensionRequestDTO requestTimeExtension(Long childId, int minutes, String reason);
    List<TimeExtensionRequestDTO> getExtensionRequests(Long userId);
    TimeExtensionRequestDTO approveExtension(Long parentId, Long requestId, int minutesToGrant, String message);
    TimeExtensionRequestDTO denyExtension(Long parentId, Long requestId, String message);

    // Validation helpers
    boolean isParentOf(Long parentId, Long childId);
    boolean isChildAccount(Long userId);
    boolean requiresParentalApproval(Long childId, String actionType, Object details);
}
