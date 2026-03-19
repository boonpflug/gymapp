package com.gymplatform.modules.training;

import org.springframework.data.jpa.domain.Specification;

public class ExerciseSpecification {

    private ExerciseSpecification() {}

    public static Specification<Exercise> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) return null;
            String pattern = "%" + name.toLowerCase() + "%";
            return cb.like(cb.lower(root.get("name")), pattern);
        };
    }

    public static Specification<Exercise> hasMuscleGroup(MuscleGroup muscleGroup) {
        return (root, query, cb) -> {
            if (muscleGroup == null) return null;
            return cb.or(
                    cb.equal(root.get("primaryMuscleGroup"), muscleGroup),
                    cb.equal(root.get("secondaryMuscleGroup"), muscleGroup)
            );
        };
    }

    public static Specification<Exercise> hasExerciseType(ExerciseType exerciseType) {
        return (root, query, cb) -> {
            if (exerciseType == null) return null;
            return cb.equal(root.get("exerciseType"), exerciseType);
        };
    }

    public static Specification<Exercise> hasEquipment(String equipment) {
        return (root, query, cb) -> {
            if (equipment == null || equipment.isBlank()) return null;
            return cb.like(cb.lower(root.get("equipment")), "%" + equipment.toLowerCase() + "%");
        };
    }

    public static Specification<Exercise> isActive() {
        return (root, query, cb) -> cb.equal(root.get("active"), true);
    }

    public static Specification<Exercise> isAvailableForTenant(String tenantId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("global"), true),
                cb.equal(root.get("tenantId"), tenantId)
        );
    }
}
