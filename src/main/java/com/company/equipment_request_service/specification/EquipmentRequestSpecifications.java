package com.company.equipment_request_service.specification;

import com.company.equipment_request_service.entity.EquipmentRequest;
import com.company.equipment_request_service.entity.RequestStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class EquipmentRequestSpecifications {

    private EquipmentRequestSpecifications() {}

    public static Specification<EquipmentRequest> byStatus(RequestStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<EquipmentRequest> byAuthor(Long authorId) {
        return (root, query, cb) -> cb.equal(root.get("author").get("id"), authorId);
    }

    public static Specification<EquipmentRequest> byCreatedBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            Predicate ge = cb.greaterThanOrEqualTo(root.get("createdAt"), from); // created_at >= from
            Predicate le = cb.lessThanOrEqualTo(root.get("createdAt"), to);      // created_at <= to
            return cb.and(ge, le);
        };
    }

    public static Specification<EquipmentRequest> pendingFor(Long currentUserId) {
        return (root, query, cb) -> {
            Predicate reviewing = cb.equal(root.get("status"), RequestStatus.UNDER_REVIEW);
            Predicate notMine = cb.notEqual(root.get("author").get("id"), currentUserId);
            return cb.and(reviewing, notMine);
        };
    }
}
