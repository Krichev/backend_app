package com.my.challenger.web.controllers;

import com.my.challenger.dto.parental.*;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.ParentalControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/parental")
@RequiredArgsConstructor
@Tag(name = "Parental Controls", description = "Parent-child account management and controls")
public class ParentalControlController {

    private final ParentalControlService parentalService;

    // ========== LINK MANAGEMENT ==========

    @PostMapping("/link")
    @Operation(summary = "Request to link a child account")
    public ResponseEntity<ParentalLinkDTO> requestLink(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody LinkChildRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long parentId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(parentalService.requestLink(parentId, request.getChildUserId()));
    }

    @PostMapping("/link/{linkId}/accept")
    @Operation(summary = "Child accepts parent link")
    public ResponseEntity<ParentalLinkDTO> acceptLink(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long linkId,
            @RequestBody AcceptLinkRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long childId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(parentalService.acceptLink(childId, linkId, request.getVerificationCode()));
    }

    @PostMapping("/link/{linkId}/reject")
    @Operation(summary = "Child rejects parent link")
    public ResponseEntity<Void> rejectLink(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long linkId) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long childId = ((UserPrincipal) userDetails).getId();
        parentalService.rejectLink(childId, linkId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/children")
    @Operation(summary = "Get parent's linked children")
    public ResponseEntity<List<ParentalLinkDTO>> getLinkedChildren(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long parentId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(parentalService.getLinkedChildren(parentId));
    }

    @GetMapping("/parents")
    @Operation(summary = "Get child's linked parents")
    public ResponseEntity<List<ParentalLinkDTO>> getLinkedParents(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long childId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(parentalService.getLinkedParents(childId));
    }

    // ========== CHILD SETTINGS ==========

    @GetMapping("/children/{childId}/settings")
    @Operation(summary = "Get child's settings (parent only)")
    public ResponseEntity<ChildSettingsDTO> getChildSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long childId) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long parentId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(parentalService.getChildSettings(parentId, childId));
    }

    @PutMapping("/children/{childId}/settings")
    @Operation(summary = "Update child's settings (parent only)")
    public ResponseEntity<ChildSettingsDTO> updateChildSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long childId,
            @Valid @RequestBody UpdateChildSettingsRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long parentId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(parentalService.updateChildSettings(parentId, childId, request));
    }

    @GetMapping("/children/{childId}/screen-time")
    @Operation(summary = "Get child's screen time status (parent only)")
    public ResponseEntity<ChildScreenTimeDTO> getChildScreenTime(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long childId) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long parentId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(parentalService.getChildScreenTime(parentId, childId));
    }

    // ========== APPROVALS ==========

    @GetMapping("/approvals/pending")
    @Operation(summary = "Get pending approvals for parent")
    public ResponseEntity<List<ParentalApprovalDTO>> getPendingApprovals(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long parentId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(parentalService.getPendingApprovals(parentId));
    }

    @PostMapping("/approvals/{approvalId}/approve")
    @Operation(summary = "Approve a pending request")
    public ResponseEntity<ParentalApprovalDTO> approveRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long approvalId,
            @RequestBody ApprovalResponseRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long parentId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(parentalService.approveRequest(parentId, approvalId, request));
    }

    @PostMapping("/approvals/{approvalId}/deny")
    @Operation(summary = "Deny a pending request")
    public ResponseEntity<ParentalApprovalDTO> denyRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long approvalId,
            @RequestBody ApprovalResponseRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long parentId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(parentalService.denyRequest(parentId, approvalId, request));
    }

    // ========== TIME EXTENSIONS ==========

    @PostMapping("/extensions/{requestId}/approve")
    @Operation(summary = "Approve time extension request")
    public ResponseEntity<TimeExtensionRequestDTO> approveExtension(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @Valid @RequestBody ApproveExtensionRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long parentId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(parentalService.approveExtension(
                parentId, requestId, request.getMinutesToGrant(), request.getMessage()));
    }

    @PostMapping("/extensions/{requestId}/deny")
    @Operation(summary = "Deny time extension request")
    public ResponseEntity<TimeExtensionRequestDTO> denyExtension(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @RequestBody DenyExtensionRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long parentId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(parentalService.denyExtension(parentId, requestId, request.getMessage()));
    }
}
