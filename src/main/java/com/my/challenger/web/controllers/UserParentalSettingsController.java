package com.my.challenger.web.controllers;

import com.my.challenger.dto.parental.*;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.impl.UserParentalSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/parental-settings")
@RequiredArgsConstructor
@Tag(name = "Parental Settings", description = "Manage parental controls and child account linking")
public class UserParentalSettingsController {

    private final UserParentalSettingsService parentalService;

    @GetMapping
    @Operation(summary = "Get parental settings for current user",
               description = "Returns user's parental control settings. Creates defaults if none exist.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Settings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserParentalSettingsDTO> getMySettings(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((UserPrincipal) userDetails).getId();
        log.debug("GET /api/parental-settings - userId: {}", userId);
        return ResponseEntity.ok(parentalService.getOrCreateSettings(userId));
    }

    @PutMapping
    @Operation(summary = "Update parental settings",
               description = "Partially updates user's parental settings. Only provided fields are updated.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Settings updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserParentalSettingsDTO> updateSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateParentalSettingsRequest request) {
        Long userId = ((UserPrincipal) userDetails).getId();
        log.debug("PUT /api/parental-settings - userId: {}", userId);
        return ResponseEntity.ok(parentalService.updateSettings(userId, request));
    }

    @PostMapping("/link-child")
    @Operation(summary = "Link a child account",
               description = "Links another user account as a child account under the current user's parental control.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Child account linked successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or already linked"),
        @ApiResponse(responseCode = "404", description = "Child user not found")
    })
    public ResponseEntity<ChildAccountDTO> linkChildAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody LinkChildRequest request) {
        Long parentUserId = ((UserPrincipal) userDetails).getId();
        log.info("POST /api/parental-settings/link-child - parent: {}, child: {}", 
                 parentUserId, request.getChildUserId());
        return ResponseEntity.ok(parentalService.linkChildAccount(parentUserId, request));
    }

    @DeleteMapping("/unlink-child/{childId}")
    @Operation(summary = "Unlink a child account",
               description = "Removes a child account from parental control.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Child account unlinked successfully"),
        @ApiResponse(responseCode = "400", description = "Not the parent of this child"),
        @ApiResponse(responseCode = "404", description = "Child settings not found")
    })
    public ResponseEntity<Void> unlinkChildAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long childId) {
        Long parentUserId = ((UserPrincipal) userDetails).getId();
        log.info("DELETE /api/parental-settings/unlink-child/{} - parent: {}", childId, parentUserId);
        parentalService.unlinkChildAccount(parentUserId, childId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/children")
    @Operation(summary = "Get linked child accounts",
               description = "Returns all child accounts linked to the current user.")
    public ResponseEntity<List<ChildAccountDTO>> getLinkedChildren(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long parentUserId = ((UserPrincipal) userDetails).getId();
        log.debug("GET /api/parental-settings/children - parent: {}", parentUserId);
        return ResponseEntity.ok(parentalService.getLinkedChildren(parentUserId));
    }

    @PutMapping("/children/{childId}")
    @Operation(summary = "Update child account settings",
               description = "Updates parental control settings for a specific linked child account.")
    public ResponseEntity<ChildAccountDTO> updateChildSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long childId,
            @Valid @RequestBody UpdateParentalSettingsRequest request) {
        Long parentUserId = ((UserPrincipal) userDetails).getId();
        log.info("PUT /api/parental-settings/children/{} - parent: {}", childId, parentUserId);
        return ResponseEntity.ok(parentalService.updateChildSettings(parentUserId, childId, request));
    }

    @PostMapping("/verify-pin")
    @Operation(summary = "Verify parent PIN",
               description = "Verifies the parent PIN for accessing restricted settings.")
    public ResponseEntity<Map<String, Boolean>> verifyPin(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        Long userId = ((UserPrincipal) userDetails).getId();
        String pin = request.get("pin");
        
        if (pin == null || pin.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("valid", false));
        }
        
        boolean valid = parentalService.verifyParentPin(userId, pin);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @GetMapping("/is-child")
    @Operation(summary = "Check if current user is a child account")
    public ResponseEntity<Map<String, Boolean>> isChildAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((UserPrincipal) userDetails).getId();
        boolean isChild = parentalService.isChildAccount(userId);
        return ResponseEntity.ok(Map.of("isChildAccount", isChild));
    }
}
