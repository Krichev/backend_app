package com.my.challenger.web.controllers;

import com.my.challenger.dto.ChallengeDTO;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.ChallengeStatus;
import com.my.challenger.entity.enums.ChallengeType;
import com.my.challenger.entity.enums.PaymentType;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.impl.ChallengeSearchService;
import com.my.challenger.service.impl.ChallengeSearchService.ChallengeSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Challenge Search
 */
@RestController
@RequestMapping("/challenges/search")
@RequiredArgsConstructor
@Slf4j
public class ChallengeSearchController {

    private final ChallengeSearchService searchService;
    private final UserRepository userRepository;

    /**
     * Advanced search with multiple filters
     */
    @GetMapping("/advanced")
    public ResponseEntity<List<ChallengeDTO>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentType,
            @RequestParam(required = false) Boolean freeOnly,
            @RequestParam(required = false) Boolean paidOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);

        ChallengeSearchRequest request = new ChallengeSearchRequest();
        request.setKeyword(keyword);
        request.setType(type != null ? ChallengeType.valueOf(type) : null);
        request.setStatus(status != null ? ChallengeStatus.valueOf(status) : null);
        request.setPaymentType(paymentType != null ? PaymentType.valueOf(paymentType) : null);
        request.setFreeOnly(freeOnly);
        request.setPaidOnly(paidOnly);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        List<ChallengeDTO> results = searchService.searchChallenges(request, user.getId());
        return ResponseEntity.ok(results);
    }

    /**
     * Search public challenges only
     */
    @GetMapping("/public")
    public ResponseEntity<List<ChallengeDTO>> searchPublicChallenges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        List<ChallengeDTO> results = searchService.getPublicChallenges(page, size, user.getId());
        return ResponseEntity.ok(results);
    }

    /**
     * Get private challenges user has access to
     */
    @GetMapping("/private")
    public ResponseEntity<List<ChallengeDTO>> getPrivateChallenges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        List<ChallengeDTO> results = searchService.getPrivateChallengesForUser(
                user.getId(), page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Search free challenges
     */
    @GetMapping("/free")
    public ResponseEntity<List<ChallengeDTO>> searchFreeChallenges(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        List<ChallengeDTO> results = searchService.searchFreeChallenges(
                keyword, user.getId(), page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Search paid challenges
     */
    @GetMapping("/paid")
    public ResponseEntity<List<ChallengeDTO>> searchPaidChallenges(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        List<ChallengeDTO> results = searchService.searchPaidChallenges(
                keyword, user.getId(), page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Get recommended challenges
     */
    @GetMapping("/recommended")
    public ResponseEntity<List<ChallengeDTO>> getRecommendedChallenges(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        List<ChallengeDTO> results = searchService.getRecommendedChallenges(
                user.getId(), limit);
        return ResponseEntity.ok(results);
    }

    /**
     * Search by challenge type/category
     */
    @GetMapping("/by-type/{type}")
    public ResponseEntity<List<ChallengeDTO>> searchByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUserFromUserDetails(userDetails);
        
        try {
            ChallengeType challengeType = ChallengeType.valueOf(type.toUpperCase());
            List<ChallengeDTO> results = searchService.searchByType(
                    challengeType, user.getId(), page, size);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Helper method
    private User getUserFromUserDetails(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}