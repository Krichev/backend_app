package com.my.challenger.repository.specification;

import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.ChallengeStatus;
import com.my.challenger.entity.enums.ChallengeType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Specification class for dynamic Challenge filtering
 * This solves the PostgreSQL ENUM NULL parameter issue
 */
public class ChallengeSpecification {

    /**
     * Build dynamic specification based on filter parameters
     */
    public static Specification<Challenge> withFilters(
            ChallengeType type,
            Boolean visibility,
            ChallengeStatus status,
            String targetGroup
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by type (only if not null)
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            // Filter by visibility (only if not null)
            if (visibility != null) {
                predicates.add(criteriaBuilder.equal(root.get("isPublic"), visibility));
            }

            // Filter by status (only if not null)
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // Filter by target group (only if not null)
            if (targetGroup != null && !targetGroup.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                    root.join("group").get("name"), targetGroup
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Additional specifications for common filters
     */
    public static Specification<Challenge> isPublic() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.isTrue(root.get("isPublic"));
    }

    public static Specification<Challenge> isActive() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("status"), ChallengeStatus.ACTIVE);
    }

    public static Specification<Challenge> hasType(ChallengeType type) {
        return (root, query, criteriaBuilder) ->
            type == null ? criteriaBuilder.conjunction() :
            criteriaBuilder.equal(root.get("type"), type);
    }

    public static Specification<Challenge> hasStatus(ChallengeStatus status) {
        return (root, query, criteriaBuilder) ->
            status == null ? criteriaBuilder.conjunction() :
            criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Challenge> hasVisibility(Boolean isPublic) {
        return (root, query, criteriaBuilder) ->
            isPublic == null ? criteriaBuilder.conjunction() :
            criteriaBuilder.equal(root.get("isPublic"), isPublic);
    }

    public static Specification<Challenge> inGroup(String groupName) {
        return (root, query, criteriaBuilder) -> {
            if (groupName == null || groupName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.join("group").get("name"), groupName
            );
        };
    }
}