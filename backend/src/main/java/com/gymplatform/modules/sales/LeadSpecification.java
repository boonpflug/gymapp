package com.gymplatform.modules.sales;

import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class LeadSpecification {

    private LeadSpecification() {}

    public static Specification<Lead> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) return null;
            String pattern = "%" + name.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern)
            );
        };
    }

    public static Specification<Lead> hasSource(LeadSource source) {
        return (root, query, cb) -> {
            if (source == null) return null;
            return cb.equal(root.get("source"), source);
        };
    }

    public static Specification<Lead> hasStage(UUID stageId) {
        return (root, query, cb) -> {
            if (stageId == null) return null;
            return cb.equal(root.get("stageId"), stageId);
        };
    }

    public static Specification<Lead> hasAssignedStaff(UUID staffId) {
        return (root, query, cb) -> {
            if (staffId == null) return null;
            return cb.equal(root.get("assignedStaffId"), staffId);
        };
    }

    public static Specification<Lead> notConverted() {
        return (root, query, cb) -> cb.isNull(root.get("convertedMemberId"));
    }
}
