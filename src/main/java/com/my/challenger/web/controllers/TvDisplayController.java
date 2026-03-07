package com.my.challenger.web.controllers;

import com.my.challenger.dto.tv.*;
import com.my.challenger.entity.User;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.impl.TvDisplayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tv-displays")
@RequiredArgsConstructor
@Slf4j
public class TvDisplayController {

    private final TvDisplayService tvDisplayService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<TvDisplayRegistrationDTO> register() {
        log.info("Registering new TV display");
        return ResponseEntity.ok(tvDisplayService.register());
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<TvDisplayStatusDTO> getStatus(@PathVariable Long id) {
        log.debug("Checking status for TV display ID: {}", id);
        return ResponseEntity.ok(tvDisplayService.getStatus(id));
    }

    @PostMapping("/claim")
    public ResponseEntity<TvDisplayClaimDTO> claim(
            @Valid @RequestBody TvDisplayClaimRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User host = getUserFromUserDetails(userDetails);
        log.info("Claiming TV display with code: {} for room: {} by host: {}", 
                request.getPairingCode(), request.getRoomCode(), host.getUsername());
        
        return ResponseEntity.ok(tvDisplayService.claim(
                request.getPairingCode(), request.getRoomCode(), host.getId()));
    }

    private User getUserFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("User not authenticated");
        }

        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
