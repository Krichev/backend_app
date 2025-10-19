package com.my.challenger.service.impl;

import com.my.challenger.dto.ChallengeDTO;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.ChallengeStatus;
import com.my.challenger.entity.enums.ChallengeType;
import com.my.challenger.entity.enums.PaymentType;
import com.my.challenger.repository.ChallengeAccessRepository;
import com.my.challenger.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Advanced Challenge Search Service with Access Control
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChallengeSearchService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeAccessRepository accessRepository;

    /**
     * Search challenges with keyword and filters
     */
    public List<ChallengeDTO> searchChallenges(ChallengeSearchRequest request, Long userId) {
        log.info("Searching challenges for user {} with filters: {}", userId, request);

        List<Challenge> challenges;

        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            // Keyword search
            challenges = challengeRepository.searchByKeyword(request.getKeyword());
        } else {
            // Get all challenges
            Pageable pageable = createPageable(request);
            Page<Challenge> page = challengeRepository.findAll(pageable);
            challenges = page.getContent();
        }

        // Apply filters
        challenges = applyFilters(challenges, request);

        // Filter by access control
        challenges = filterByAccess(challenges, userId);

        // Convert to DTOs
        return challenges.stream()
                .map(challenge -> convertToDTO(challenge, userId))
                .collect(Collectors.toList());
    }

    /**
     * Get public challenges only
     */
    public List<ChallengeDTO> getPublicChallenges(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
        
        List<Challenge> challenges = challengeRepository.findAll(pageable).getContent()
                .stream()
                .filter(Challenge::isPublic)
                .filter(c -> c.getStatus() == ChallengeStatus.ACTIVE)
                .collect(Collectors.toList());

        return challenges.stream()
                .map(challenge -> convertToDTO(challenge, userId))
                .collect(Collectors.toList());
    }

    /**
     * Get private challenges user has access to
     */
    public List<ChallengeDTO> getPrivateChallengesForUser(Long userId, int page, int size) {
        return accessRepository.findActiveByUserId(userId).stream()
                .map(access -> access.getChallenge())
                .filter(challenge -> !challenge.isPublic())
                .map(challenge -> convertToDTO(challenge, userId))
                .collect(Collectors.toList());
    }

    /**
     * Search free challenges
     */
    public List<ChallengeDTO> searchFreeChallenges(String keyword, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        List<Challenge> challenges = keyword != null && !keyword.isEmpty()
                ? challengeRepository.searchByKeyword(keyword)
                : challengeRepository.findAll(pageable).getContent();

        return challenges.stream()
                .filter(c -> c.getPaymentType() == PaymentType.FREE)
                .filter(c -> canUserAccessChallenge(c, userId))
                .map(challenge -> convertToDTO(challenge, userId))
                .collect(Collectors.toList());
    }

    /**
     * Search paid challenges
     */
    public List<ChallengeDTO> searchPaidChallenges(String keyword, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        List<Challenge> challenges = keyword != null && !keyword.isEmpty()
                ? challengeRepository.searchByKeyword(keyword)
                : challengeRepository.findAll(pageable).getContent();

        return challenges.stream()
                .filter(c -> c.isHasEntryFee())
                .filter(c -> canUserAccessChallenge(c, userId))
                .map(challenge -> convertToDTO(challenge, userId))
                .collect(Collectors.toList());
    }

    /**
     * Get recommended challenges for user
     */
    public List<ChallengeDTO> getRecommendedChallenges(Long userId, int limit) {
        // Simple recommendation: return active public challenges user hasn't joined
        Pageable pageable = PageRequest.of(0, limit);
        
        return challengeRepository.findAll(pageable).getContent().stream()
                .filter(Challenge::isPublic)
                .filter(c -> c.getStatus() == ChallengeStatus.ACTIVE)
                .filter(c -> !hasUserJoined(c, userId))
                .map(challenge -> convertToDTO(challenge, userId))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Search by category/type
     */
    public List<ChallengeDTO> searchByType(ChallengeType type, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        List<Challenge> challenges = challengeRepository.findAll(pageable).getContent().stream()
                .filter(c -> c.getType() == type)
                .filter(c -> canUserAccessChallenge(c, userId))
                .collect(Collectors.toList());

        return challenges.stream()
                .map(challenge -> convertToDTO(challenge, userId))
                .collect(Collectors.toList());
    }

    // ========== HELPER METHODS ==========

    private List<Challenge> applyFilters(List<Challenge> challenges, ChallengeSearchRequest request) {
        return challenges.stream()
                .filter(c -> request.getType() == null || c.getType() == request.getType())
                .filter(c -> request.getStatus() == null || c.getStatus() == request.getStatus())
                .filter(c -> request.getPaymentType() == null || c.getPaymentType() == request.getPaymentType())
                .filter(c -> !Boolean.TRUE.equals(request.getFreeOnly()) || c.getPaymentType() == PaymentType.FREE)
                .filter(c -> !Boolean.TRUE.equals(request.getPaidOnly()) || c.isHasEntryFee())
                .collect(Collectors.toList());
    }

    private List<Challenge> filterByAccess(List<Challenge> challenges, Long userId) {
        return challenges.stream()
                .filter(c -> canUserAccessChallenge(c, userId))
                .collect(Collectors.toList());
    }

    private boolean canUserAccessChallenge(Challenge challenge, Long userId) {
        // Public challenges are accessible to all
        if (challenge.isPublic()) {
            return true;
        }

        // Creator always has access
        if (challenge.getCreator().getId().equals(userId)) {
            return true;
        }

        // Check access list for private challenges
        return accessRepository.hasAccess(challenge.getId(), userId);
    }

    private boolean hasUserJoined(Challenge challenge, Long userId) {
        return challenge.getProgress().stream()
                .anyMatch(progress -> progress.getUser().getId().equals(userId));
    }

    private Pageable createPageable(ChallengeSearchRequest request) {
        Sort sort = Sort.by(Sort.Direction.DESC, "startDate");
        
        if (request.getSortBy() != null) {
            Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection()) 
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, request.getSortBy());
        }

        return PageRequest.of(
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 20,
                sort
        );
    }

    private ChallengeDTO convertToDTO(Challenge challenge, Long userId) {
        ChallengeDTO dto = new ChallengeDTO();
        dto.setId(challenge.getId());
        dto.setTitle(challenge.getTitle());
        dto.setDescription(challenge.getDescription());
        dto.setType(challenge.getType());
        dto.setStatus(challenge.getStatus());
        dto.setIsPublic(challenge.isPublic());
        dto.setCreator_id(challenge.getCreator().getId());
        dto.setCreatorUsername(challenge.getCreator().getUsername());
        
        // Payment info
        dto.setPaymentType(challenge.getPaymentType());
        dto.setHasEntryFee(challenge.isHasEntryFee());
        dto.setEntryFeeAmount(challenge.getEntryFeeAmount());
        dto.setEntryFeeCurrency(challenge.getEntryFeeCurrency());
        dto.setHasPrize(challenge.isHasPrize());
        dto.setPrizeAmount(challenge.getPrizeAmount());
        dto.setPrizeCurrency(challenge.getPrizeCurrency());
        dto.setPrizePool(challenge.getPrizePool());
        
        // Access info
        dto.setUserHasAccess(canUserAccessChallenge(challenge, userId));
        dto.setUserIsCreator(challenge.getCreator().getId().equals(userId));
        
        return dto;
    }

    /**
     * Search Request DTO
     */
    public static class ChallengeSearchRequest {
        private String keyword;
        private ChallengeType type;
        private ChallengeStatus status;
        private PaymentType paymentType;
        private Boolean freeOnly;
        private Boolean paidOnly;
        private Integer page;
        private Integer size;
        private String sortBy;
        private String sortDirection;

        // Getters and Setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public ChallengeType getType() { return type; }
        public void setType(ChallengeType type) { this.type = type; }
        public ChallengeStatus getStatus() { return status; }
        public void setStatus(ChallengeStatus status) { this.status = status; }
        public PaymentType getPaymentType() { return paymentType; }
        public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }
        public Boolean getFreeOnly() { return freeOnly; }
        public void setFreeOnly(Boolean freeOnly) { this.freeOnly = freeOnly; }
        public Boolean getPaidOnly() { return paidOnly; }
        public void setPaidOnly(Boolean paidOnly) { this.paidOnly = paidOnly; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
        public String getSortDirection() { return sortDirection; }
        public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
    }
}