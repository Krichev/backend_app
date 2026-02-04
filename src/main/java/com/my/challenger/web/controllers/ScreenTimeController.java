package com.my.challenger.web.controllers;

import com.my.challenger.dto.screentime.*;
import com.my.challenger.entity.User;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.ScreenTimeBudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/screen-time")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class ScreenTimeController {

    private final ScreenTimeBudgetService screenTimeBudgetService;
    private final UserRepository userRepository;

    @GetMapping("/budget")
    public ResponseEntity<ScreenTimeBudgetDTO> getBudget(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromUserDetails(userDetails);
        return ResponseEntity.ok(screenTimeBudgetService.getOrCreateBudget(user.getId()));
    }

    @PutMapping("/budget/configure")
    public ResponseEntity<ScreenTimeBudgetDTO> configureBudget(
            @Valid @RequestBody ConfigureBudgetRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromUserDetails(userDetails);
        return ResponseEntity.ok(screenTimeBudgetService.configureBudget(user.getId(), request));
    }

    @PostMapping("/budget/deduct")
    public ResponseEntity<ScreenTimeBudgetDTO> deductTime(
            @Valid @RequestBody DeductTimeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromUserDetails(userDetails);
        return ResponseEntity.ok(screenTimeBudgetService.deductTime(user.getId(), request.getMinutes()));
    }

    @PostMapping("/budget/sync")
    public ResponseEntity<ScreenTimeBudgetDTO> syncUsage(
            @Valid @RequestBody SyncTimeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromUserDetails(userDetails);
        return ResponseEntity.ok(screenTimeBudgetService.syncUsage(user.getId(), request));
    }

    @GetMapping("/budget/status")
    public ResponseEntity<ScreenTimeStatusDTO> getStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromUserDetails(userDetails);
        return ResponseEntity.ok(screenTimeBudgetService.getStatus(user.getId()));
    }

    private User getUserFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("User not authenticated");
        }
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
